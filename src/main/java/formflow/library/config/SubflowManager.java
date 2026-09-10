package formflow.library.config;

import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages the data and navigation logic for subflows - a repeatable section of a flow (e.g. "add a household
 * member") that a user can iterate through multiple times, each iteration identified by its own UUID.
 *
 * <p>
 * This includes {@link SubflowRelationship subflow relationships} (keeping one subflow's iterations in sync with
 * another related subflow's, optionally filtered), and {@link RepeatFor repeatFor} iterations (nested, per-item
 * iteration data saved underneath a single subflow entry, e.g. one sub-iteration per item a user selected in a
 * checkbox set).
 * </p>
 */
@Component
public class SubflowManager {

    private final SubflowFilterManager subflowFilterManager;
    List<FlowConfiguration> flowConfigurations;

    /**
     * Wires up the flow configuration and filter manager this class needs.
     *
     * @param flowConfigurations   the configured flows for this application
     * @param subflowFilterManager manager used to run a named {@link formflow.library.config.submission.SubflowRelationshipFilter} against related
     *                             subflow data
     */
    public SubflowManager(List<FlowConfiguration> flowConfigurations, SubflowFilterManager subflowFilterManager) {
        this.flowConfigurations = flowConfigurations;
        this.subflowFilterManager = subflowFilterManager;
    }

    /**
     * Checks whether a subflow is configured with a {@link SubflowRelationship} to another subflow.
     *
     * @param flow    the flow the subflow belongs to
     * @param subflow the subflow to check
     * @return true if the subflow has a relationship configured
     * @throws ResponseStatusException if the subflow isn't found in the flow's configuration
     */
    public boolean subflowHasRelationship(String flow, String subflow) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flow);
        if (flowConfiguration.getSubflows().containsKey(subflow)) {
            return flowConfiguration.getSubflows().get(subflow).getRelationship() != null;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Check that your flows-config.yaml is configured correctly.",
                            subflow, flow));
        }
    }

    /**
     * Keeps a subflow's iterations in sync with the related subflow it's configured to relate to.
     *
     * <p>
     * If this is the first time the related subflow's items have been seen, an incomplete iteration is added here
     * for each of the related subflow's items. If the related subflow's item count no longer matches (e.g. an
     * item was deleted from it), any iterations here for items that no longer exist in the related subflow are
     * left as-is, and a new incomplete iteration is added for each related item that doesn't have one yet. The
     * related subflow's data may first be run through a configured {@link formflow.library.config.submission.SubflowRelationshipFilter}.
     * </p>
     *
     * @param currentScreen the screen navigation configuration for the subflow's current screen
     * @param flow          the flow the subflow belongs to
     * @param submission    the submission to read/update the subflow data on
     */
    public void addSubflowRelationshipData(ScreenNavigationConfiguration currentScreen, String flow, Submission submission) {
        String subflowName = currentScreen.getSubflow();

        SubflowConfiguration currentSubflow = getSubflowConfiguration(flow, subflowName);
        String relatedSubflowName = currentSubflow.getRelationship().getRelatesTo();
        String relatedIdKey = currentSubflow.getRelationship().getRelationAlias();

        List<HashMap<String, Object>> relatedSubflowData = getSubflowData(submission, relatedSubflowName);
        List<Map<String, Object>> currentSubflowData = getOrCreateSubflowData(submission, subflowName);

        if (subflowHasRelationshipFilter(flow, currentScreen.getSubflow())) {
            List<HashMap<String, Object>> copyOfSubflowDataToFilterAgainst = relatedSubflowData.stream().map(HashMap::new)
                    .toList();
            relatedSubflowData = handleSubflowRelationshipFilter(flow, currentScreen.getSubflow(),
                    copyOfSubflowDataToFilterAgainst, submission);
        }

        if (!submission.getInputData().containsKey(subflowName)) {
            // Initial setup: add all related items as incomplete iterations
            relatedSubflowData.forEach(relatedItem ->
                    currentSubflowData.add(createSubflowIterationWithRelationship(relatedIdKey, relatedItem.get("uuid")))
            );
        } else if (currentSubflowData.size() != relatedSubflowData.size()) {
            // Reconciliation: we must have deleted some iterations, we need to reset the relationships and iteration statuses
            // Collect the existing iteration IDs and then loop over the related subflow to find the missing iteration IDs
            Set<Object> existingRelationIds = currentSubflowData.stream()
                    .map(entry -> entry.get(relatedIdKey))
                    .collect(Collectors.toSet());

            relatedSubflowData.stream()
                    .map(item -> item.get("uuid"))
                    .filter(uuid -> !existingRelationIds.contains(uuid))
                    .forEach(missingUuid ->
                            currentSubflowData.add(createSubflowIterationWithRelationship(relatedIdKey, missingUuid))
                    );
        }
    }

    /**
     * Checks whether every iteration of a subflow has been marked complete.
     *
     * @param currentSubflowName the subflow to check
     * @param submission         the submission to read the subflow's iterations from
     * @return true if the subflow has iterations and all of them are complete
     */
    public boolean hasFinishedAllSubflowIterations(String currentSubflowName, Submission submission) {
        // Only read here, so a wildcard cast - fully checked, no unchecked warning - is enough.
        List<?> currentSubflowData = (List<?>) submission.getInputData().get(currentSubflowName);

        return currentSubflowData.stream()
                .map(iteration -> (Map<?, ?>) iteration)
                .allMatch(iteration -> iteration.get(Submission.ITERATION_IS_COMPLETE_KEY).equals(true));
    }

    /**
     * Looks up the configured screen a user lands on when starting a new iteration of a subflow.
     *
     * @param flowName    the flow the subflow belongs to
     * @param subflowName the subflow to look up
     * @return the subflow's configured iteration start screen name
     * @throws ResponseStatusException if the subflow isn't found in the flow's configuration
     */
    public String getIterationStartScreenForSubflow(String flowName, String subflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);
        if (flowConfiguration.getSubflows().get(subflowName) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Check that your flows-config.yaml is configured correctly.",
                            subflowName, flowName));
        }
        return flowConfiguration.getSubflows().get(subflowName).getIterationStartScreen();
    }

    /**
     * Looks up the name of the subflow that a given subflow is configured to relate to.
     *
     * @param flowName           the flow the subflow belongs to
     * @param currentSubflowName the subflow whose relationship should be looked up
     * @return the related subflow's name
     * @throws ResponseStatusException if the subflow isn't found in the flow's configuration; note that calling
     *                                 this on a subflow with no {@link SubflowRelationship} configured at all will
     *                                 throw a NullPointerException instead
     */
    public String getRelatedSubflowName(String flowName, String currentSubflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);

        if (flowConfiguration.getSubflows().containsKey(currentSubflowName)) {
            return flowConfiguration.getSubflows().get(currentSubflowName).getRelationship().getRelatesTo();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format(
                            "Subflow %s not found in flow %s. Is it possible you entered an incorrect subflow name in your subflow configuration?",
                            currentSubflowName, flowName));
        }
    }

    /**
     * Given one subflow iteration, looks up the corresponding iteration of the subflow it's related to.
     *
     * @param flowName    the flow the subflow belongs to
     * @param subflowName the subflow containing the iteration to start from
     * @param iterationId the UUID of the iteration to start from
     * @param submission  the submission to read the subflow data from
     * @return the related subflow's matching iteration data
     */
    public Map<String, Object> getRelatedSubflowIteration(String flowName, String subflowName, String iterationId,
            Submission submission) {
        Map<String, Object> currentSubflowEntry = submission.getSubflowEntryByUuid(subflowName, iterationId);
        String relationKey = getRelationKey(flowName, subflowName);
        String relatedIterationId = (String) currentSubflowEntry.get(relationKey);
        String relatedSubflowName = getRelatedSubflowName(flowName, subflowName);
        return submission.getSubflowEntryByUuid(relatedSubflowName, relatedIterationId);
    }

    /**
     * Looks up a subflow's configuration.
     *
     * @param flow    the flow the subflow belongs to
     * @param subflow the subflow to look up
     * @return the subflow's configuration
     * @throws ResponseStatusException if the subflow isn't found in the flow's configuration
     */
    public SubflowConfiguration getSubflowConfiguration(String flow, String subflow) {
        SubflowConfiguration subflowConfiguration = flowConfigurations.stream()
                .filter(config -> config.getName().equals(flow))
                .findFirst().get().getSubflows().get(subflow);

        if (subflowConfiguration == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format(
                            "Subflow %s not found in flow configuration for flow: %s. Check that your flows-config.yaml is configured correctly.",
                            subflow, flow));
        }

        return subflowConfiguration;
    }

    /**
     * Determines which subflow iteration the user should land on when starting/continuing a subflow that has a
     * relationship to another subflow.
     *
     * <p>
     * Normally this is the next iteration that hasn't been marked complete yet (forward progress through the
     * subflow). If every iteration is already complete, but the {@code Referer} header shows the user came from
     * a specific iteration's URL, that iteration's UUID is returned instead - this covers back-navigation, where
     * a user returns to an already-completed iteration.
     * </p>
     *
     * @param referer     the value of the request's {@code Referer} header, used to detect back-navigation
     * @param subflowName the subflow to find an iteration in
     * @param submission  the submission to read the subflow's iterations from
     * @return the UUID of the iteration to land on, or null if every iteration is complete and the referer doesn't
     *         point at one of them
     */
    public String getUuidOfIterationToUpdate(String referer, String subflowName, Submission submission) {
        // Only read here, so a wildcard cast - fully checked, no unchecked warning - is enough.
        List<?> subflowData = (List<?>) submission.getInputData().get(subflowName);

        // Try to find the next incomplete iteration
        var nextIteration = subflowData.stream()
                .map(iteration -> (Map<?, ?>) iteration)
                .filter(iteration -> Boolean.FALSE.equals(iteration.get(Submission.ITERATION_IS_COMPLETE_KEY)))
                .findFirst();

        if (nextIteration.isPresent()) {
            return nextIteration.get().get("uuid").toString(); // normal forward flow
        }

        // If all iterations are complete, but referer includes a UUID, fallback to that UUID (likely back nav)
        if (isReferedFromSubflowIteration(referer)) {
            String refererUuid = extractUuidFromReferer(referer);
            if (refererUuid != null && subflowData.stream()
                    .map(iteration -> (Map<?, ?>) iteration)
                    .anyMatch(i -> refererUuid.equals(i.get("uuid")))) {
                return refererUuid; // back navigation – safe fallback
            }
        }

        return null; // no incomplete iteration found and no referer UUID to fall back on
    }

    /**
     * Checks whether a {@code Referer} header value looks like a subflow iteration's URL (ending in
     * {@code /<screenName>/<uuid>}).
     *
     * @param referer the value of the request's {@code Referer} header, may be null
     * @return true if the referer matches a subflow iteration URL pattern
     */
    public boolean isReferedFromSubflowIteration(String referer) {
        if (referer == null) {
            return false;
        }
        // Check if the URL matches pattern /<screenName>/<UUID>
        Pattern refererPattern = Pattern.compile(
                ".*/[^/]+/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );

        return refererPattern.matcher(referer).matches();
    }

    /**
     * Extracts the trailing UUID from a {@code Referer} header value that points at a subflow iteration's URL.
     *
     * @param referer the value of the request's {@code Referer} header, may be null
     * @return the UUID at the end of the referer URL, or null if the referer is null or doesn't match
     */
    public String extractUuidFromReferer(String referer) {
        if (referer == null) {
            return null;
        }

        Pattern uuidPattern = Pattern.compile(
                ".*/[^/]+/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$"
        );

        Matcher matcher = uuidPattern.matcher(referer);
        if (matcher.matches()) {
            return matcher.group(1); // return the captured UUID
        }

        return null; // no UUID match found
    }

    private FlowConfiguration getFlowConfiguration(String flowName) {
        FlowConfiguration flowConfiguration = flowConfigurations.stream()
                .filter(config -> config.getName().equals(flowName))
                .findFirst()
                .orElse(null);

        if (flowConfiguration == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Flow %s not found in your flows-config yaml file.", flowName));
        }
        return flowConfiguration;
    }

    private String getRelationKey(String flow, String subflow) {
        return flowConfigurations.stream()
                .filter(config -> config.getName().equals(flow))
                .findFirst()
                .map(config -> config.getSubflows().get(subflow))
                .map(SubflowConfiguration::getRelationship)
                .map(SubflowRelationship::getRelationAlias).orElse("relatedId");
    }

    private List<HashMap<String, Object>> getSubflowData(Submission submission, String subflowName) {
        // inputData is stored as Map<String, Object>; a subflow entry is only known to be a List, not
        // specifically List<HashMap<String,Object>> - callers need that concrete type, though.
        @SuppressWarnings("unchecked")
        List<HashMap<String, Object>> subflowData =
                (List<HashMap<String, Object>>) submission.getInputData().getOrDefault(subflowName, new ArrayList<>());
        return subflowData;
    }

    private List<Map<String, Object>> getOrCreateSubflowData(Submission submission, String subflowName) {
        // inputData is stored as Map<String, Object>; a subflow entry is only known to be a List, not
        // specifically List<Map<String,Object>> - callers mutate the result via add(), so it needs that
        // concrete type.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subflowData = (List<Map<String, Object>>) submission.getInputData()
                .computeIfAbsent(subflowName, k -> new ArrayList<Map<String, Object>>());
        return subflowData;
    }

    private Map<String, Object> createSubflowIterationWithRelationship(String relationKey, Object relatedUuid) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("uuid", UUID.randomUUID().toString());
        entry.put(relationKey, relatedUuid);
        entry.put(Submission.ITERATION_IS_COMPLETE_KEY, false);
        return entry;
    }

    /**
     * Checks whether a subflow's {@link SubflowRelationship} has a filter configured to narrow down which items
     * of the related subflow it should track.
     *
     * @param flowName    the flow the subflow belongs to
     * @param subflowName the subflow to check
     * @return true if the subflow has a relationship with a filter configured
     * @throws IllegalArgumentException if the subflow doesn't exist in the flow's configuration
     */
    public Boolean subflowHasRelationshipFilter(String flowName, String subflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);

        SubflowConfiguration subflowConfiguration = flowConfiguration.getSubflows().get(subflowName);
        if (subflowConfiguration == null) {
            throw new IllegalArgumentException("Subflow " + subflowName + " does not exist in flow " + flowName);
        }

        return subflowConfiguration.getRelationship() != null && subflowConfiguration.getRelationship().getFilter() != null;
    }

    /**
     * Runs a subflow relationship's configured {@link formflow.library.config.submission.SubflowRelationshipFilter} against the related subflow's
     * data, narrowing it down to only the items this subflow should track.
     *
     * @param flowName           the flow the subflow belongs to
     * @param subflowName        the subflow whose relationship filter should be run
     * @param subflowDataToFilter the related subflow's data to filter
     * @param submission         the submission the data belongs to, passed through to the filter
     * @return the filtered subset of {@code subflowDataToFilter}
     */
    public List<HashMap<String, Object>> handleSubflowRelationshipFilter(String flowName, String subflowName,
            List<HashMap<String, Object>> subflowDataToFilter, Submission submission) {
        SubflowConfiguration subflowConfiguration = getSubflowConfiguration(flowName, subflowName);
        String filterName = subflowConfiguration.getRelationship().getFilter();
        return subflowFilterManager.runFilter(subflowDataToFilter, filterName, submission);
    }

    /**
     * Looks up a subflow's configured relationship to another subflow, if it has one.
     *
     * @param flowName    the flow the subflow belongs to
     * @param subflowName the subflow to look up
     * @return the subflow's relationship, or empty if the subflow isn't found or has no relationship configured
     */
    public Optional<SubflowRelationship> subflowRelationship(String flowName, String subflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);

        if (flowConfiguration.getSubflows().containsKey(subflowName)) {
            SubflowRelationship subflowRelationship = flowConfiguration.getSubflows().get(subflowName).getRelationship();
            if (subflowRelationship != null) {
                return Optional.of(subflowRelationship);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates or updates a subflow iteration's nested {@link RepeatFor repeatFor} iterations - one nested
     * iteration per value the user selected for the repeatFor's configured input (e.g. one nested iteration per
     * income type selected in a checkbox set).
     *
     * <p>
     * If {@code repeatForInputData} is empty, any existing repeatFor iterations are removed. Otherwise, a nested
     * iteration is created for each new value, and an existing nested iteration is preserved (rather than
     * recreated) for any value that already had one.
     * </p>
     *
     * @param submission         the submission to read/update the subflow iteration on
     * @param subflowName        the subflow the iteration belongs to
     * @param subflowUUID        the UUID of the subflow iteration to add repeatFor data to
     * @param saveAsInputName    the key the repeatFor iterations are saved under, from the repeatFor configuration
     * @param repeatForInputData the values submitted for the repeatFor's configured input, one nested iteration
     *                           per value
     */
    public void addRepeatForIterationData(Submission submission, String subflowName, String subflowUUID,
            String saveAsInputName, List<String> repeatForInputData) {
        Map<String, Object> currentSubflowData = submission.getSubflowEntryByUuid(subflowName, subflowUUID);

        if (!repeatForInputData.isEmpty()) {
            Boolean repeatRelationHasBeenSet = currentSubflowData.containsKey(saveAsInputName);

            if (!repeatRelationHasBeenSet) {
                submission.getSubflowEntryByUuid(subflowName, subflowUUID)
                        .put(saveAsInputName, setSubflowRepeatForIterations(repeatForInputData,
                                saveAsInputName));
            } else {
                submission.getSubflowEntryByUuid(subflowName, subflowUUID)
                        .put(saveAsInputName,
                                updateSubflowRepeatForIterations(currentSubflowData, repeatForInputData, saveAsInputName));
            }
        } else {
            submission.getSubflowEntryByUuid(subflowName, subflowUUID).remove(saveAsInputName);
        }

    }

    private List<Map<String, Object>> setSubflowRepeatForIterations(List<String> inputListToRepeatOn,
            String repeatForInputData) {

        List<Map<String, Object>> repeatForIterations = new ArrayList<>();

        inputListToRepeatOn.forEach(selectedValue ->
                repeatForIterations.add(
                        createSubflowIterationRepeat(selectedValue)));

        return repeatForIterations;
    }

    private List<Map<String, Object>> updateSubflowRepeatForIterations(Map<String, Object> currentSubflowData,
            List<String> repeatForInputData, String saveAsInputName) {
        // currentSubflowData is stored as Map<String, Object>; this value is only known to be a List, not
        // specifically List<Map<String,Object>> - matched entries flow into newRepeatForIterations below, which
        // needs that concrete type.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> currentRepeatForIterations = (List<Map<String, Object>>) currentSubflowData.getOrDefault(
                saveAsInputName, Collections.EMPTY_LIST);
        List<Map<String, Object>> newRepeatForIterations = new ArrayList<>();

        repeatForInputData.forEach(newEntry -> {
            Optional<Map<String, Object>> matchingIteration = currentRepeatForIterations.stream()
                    .filter(oldEntry -> oldEntry.get("repeatForValue").equals(newEntry)).findFirst();

            if (matchingIteration.isPresent()) {
                newRepeatForIterations.add(matchingIteration.get());
            } else {
                newRepeatForIterations.add(createSubflowIterationRepeat(newEntry));
            }
        });

        return newRepeatForIterations;

    }

    private Map<String, Object> createSubflowIterationRepeat(String inputDataId) {

        Map<String, Object> entry = new HashMap<>();

        entry.put("uuid", UUID.randomUUID().toString());
        entry.put("repeatForValue", inputDataId);
        entry.put(Submission.ITERATION_IS_COMPLETE_KEY, false);

        return entry;
    }

    /**
     * Checks whether every {@link RepeatFor repeatFor} nested iteration under a subflow iteration has been marked
     * complete.
     *
     * @param subflowDataKey       the key the nested repeatFor iterations are saved under
     * @param subflowIterationData the subflow iteration's data, containing the nested iterations
     * @return true if the nested iterations exist and all of them are complete
     */
    public boolean hasFinishedAllIterations(String subflowDataKey, Map<String, Object> subflowIterationData) {
        // Only read here, so a wildcard cast - fully checked, no unchecked warning - is enough.
        List<?> currentSubflowData = (List<?>) subflowIterationData.get(subflowDataKey);

        return currentSubflowData.stream()
                .map(iteration -> (Map<?, ?>) iteration)
                .allMatch(iteration -> iteration.get(Submission.ITERATION_IS_COMPLETE_KEY).equals(true));
    }

    /**
     * Looks up a specific {@link RepeatFor repeatFor} nested iteration by UUID.
     *
     * @param subflowData      the subflow iteration's data, containing the nested iterations
     * @param nestedSubflowKey the key the nested repeatFor iterations are saved under
     * @param nestedIterationId the UUID of the nested iteration to look up
     * @return the matching nested iteration's data, or null if not found
     */
    public Map<String, Object> getRepeatForIteration(Map<String, Object> subflowData,
            String nestedSubflowKey, String nestedIterationId) {

        // subflowData is stored as Map<String, Object>; this value is only known to be a List, not specifically
        // List<Map<String,Object>>, and this method's return type requires that concrete type.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nestedIterations = (List<Map<String, Object>>) subflowData.getOrDefault(nestedSubflowKey,
                Collections.EMPTY_LIST);

        Optional<Map<String, Object>> currentIteration = nestedIterations.stream()
                .filter(iteration -> iteration.get("uuid").equals(nestedIterationId)).findFirst();

        return currentIteration.isPresent() ? currentIteration.get() : null;
    }

    /**
     * Finds the next incomplete {@link RepeatFor repeatFor} nested iteration under a subflow iteration's data, if
     * any. Despite the name, this returns the nested iteration's full data (not just its UUID) - callers pull the
     * UUID out of the returned map themselves.
     *
     * @param inputKey  the key the nested repeatFor iterations are saved under
     * @param inputData the subflow iteration's data, containing the nested iterations
     * @return the next incomplete nested iteration's data, or null if none are incomplete
     */
    public Map<String, Object> getNextRepeatForIterationUuid(String inputKey, Map<String, Object> inputData) {
        // inputData is stored as Map<String, Object>; this value is only known to be a List, not specifically
        // List<Map<String,Object>>, and this method's return type requires that concrete type.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subflowData = (List<Map<String, Object>>) inputData.get(inputKey);

        // Try to find the next incomplete iteration
        Optional<Map<String, Object>> nextIteration = subflowData.stream()
                .filter(iteration -> Boolean.FALSE.equals(iteration.get(Submission.ITERATION_IS_COMPLETE_KEY)))
                .findFirst();

        if (nextIteration.isPresent()) {
            return nextIteration.get(); // normal forward flow
        }

        return null;
    }
}
