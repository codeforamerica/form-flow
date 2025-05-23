package formflow.library.config;

import formflow.library.data.Submission;
import java.util.ArrayList;
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

@Component
public class SubflowManager {

    List<FlowConfiguration> flowConfigurations;
    
    public SubflowManager(List<FlowConfiguration> flowConfigurations) {
        this.flowConfigurations = flowConfigurations;
    }

    public boolean subflowHasRelationship(String flow, String subflow) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flow);
        if (flowConfiguration.getSubflows().containsKey(subflow)) {
            return flowConfiguration.getSubflows().get(subflow).getRelationship() != null;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Check that your flows-config.yaml is configured correctly.", subflow, flow));
        }
    }
    
    public void addSubflowRelationshipData(ScreenNavigationConfiguration currentScreen, String flow, Submission submission) {
        String subflowName = currentScreen.getSubflow();
        if (!subflowHasRelationship(flow, subflowName)) {
            // No relationship so we don't need to do anything
            return;
        }

        SubflowConfiguration currentSubflow = getSubflowConfiguration(flow, subflowName);
        String relatedSubflowName = currentSubflow.getRelationship().getRelatesTo();
        String relatedIdKey = currentSubflow.getRelationship().getRelationAlias();

        List<Map<String, Object>> relatedSubflowData = getSubflowData(submission, relatedSubflowName);
        List<Map<String, Object>> currentSubflowData = getOrCreateSubflowData(submission, subflowName);

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

    public boolean hasFinishedAllSubflowIterations(String currentSubflowName, Submission submission) {
        List<Map<String, Object>> currentSubflowData = (List<Map<String, Object>>) submission.getInputData().get(currentSubflowName);

        return currentSubflowData.stream()
                .allMatch(iteration -> iteration.get(Submission.ITERATION_IS_COMPLETE_KEY).equals(true));
    }
    
    public String getIterationStartScreenForSubflow(String flowName, String subflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);
        if (flowConfiguration.getSubflows().get(subflowName) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Check that your flows-config.yaml is configured correctly.", subflowName, flowName));
        }
        return flowConfiguration.getSubflows().get(subflowName).getIterationStartScreen();
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

    public String getRelatedSubflowName(String flowName, String currentSubflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);
        
        if (flowConfiguration.getSubflows().containsKey(currentSubflowName)) {
            return flowConfiguration.getSubflows().get(currentSubflowName).getRelationship().getRelatesTo();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Is it possible you entered an incorrect subflow name in your subflow configuration?", currentSubflowName, flowName));
        }
    }
    
    public Map<String, Object> getRelatedSubflowIteration(String flowName, String subflowName, String iterationId, Submission submission) {
        Map<String, Object> currentSubflowEntry = submission.getSubflowEntryByUuid(subflowName, iterationId);
        String relationKey = getRelationKey(flowName, subflowName);
        String relatedIterationId = (String) currentSubflowEntry.get(relationKey);
        String relatedSubflowName = getRelatedSubflowName(flowName, subflowName);
        return submission.getSubflowEntryByUuid(relatedSubflowName, relatedIterationId);
    }
    
    private String getRelationKey(String flow, String subflow) {
        return flowConfigurations.stream()
                .filter(config -> config.getName().equals(flow))
                .findFirst()
                .map(config -> config.getSubflows().get(subflow))
                .map(SubflowConfiguration::getRelationship)
                .map(SubflowRelationship::getRelationAlias).orElse("relatedId");
    }

    public SubflowConfiguration getSubflowConfiguration(String flow, String subflow) {
        return flowConfigurations.stream()
                .filter(config -> config.getName().equals(flow))
                .findFirst().get().getSubflows().get(subflow);
    }

    public String getUuidOfIterationToUpdate(String referer, String subflowName, Submission submission) {
        List<Map<String, Object>> subflowData = (List<Map<String, Object>>) submission.getInputData().get(subflowName);

        // Try to find the next incomplete iteration
        Optional<Map<String, Object>> nextIteration = subflowData.stream()
                .filter(iteration -> Boolean.FALSE.equals(iteration.get(Submission.ITERATION_IS_COMPLETE_KEY)))
                .findFirst();

        if (nextIteration.isPresent()) {
            return nextIteration.get().get("uuid").toString(); // normal forward flow
        }

        // If all iterations are complete, but referer includes a UUID, fallback to that UUID (likely back nav)
        if (isReferedFromSubflowIteration(referer)) {
            String refererUuid = extractUuidFromReferer(referer);
            if (refererUuid != null && subflowData.stream().anyMatch(i -> refererUuid.equals(i.get("uuid")))) {
                return refererUuid; // back navigation – safe fallback
            }
        }

        return null; // no incomplete iteration found and no referer UUID to fall back on
    }

    private List<Map<String, Object>> getSubflowData(Submission submission, String subflowName) {
        return (List<Map<String, Object>>) submission.getInputData().getOrDefault(subflowName, new ArrayList<>());
    }

    private List<Map<String, Object>> getOrCreateSubflowData(Submission submission, String subflowName) {
        return (List<Map<String, Object>>) submission.getInputData()
                .computeIfAbsent(subflowName, k -> new ArrayList<Map<String, Object>>());
    }

    private Map<String, Object> createSubflowIterationWithRelationship(String relationKey, Object relatedUuid) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("uuid", UUID.randomUUID().toString());
        entry.put(relationKey, relatedUuid);
        entry.put(Submission.ITERATION_IS_COMPLETE_KEY, false);
        return entry;
    }

    public boolean isReferedFromSubflowIteration(String referer) {
        if (referer == null) return false;
        // Check if the URL matches pattern /<screenName>/<UUID>
        Pattern refererPattern = Pattern.compile(
                ".*/[^/]+/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );

        return refererPattern.matcher(referer).matches();
    }

    public static String extractUuidFromReferer(String referer) {
        if (referer == null) return null;

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
}
