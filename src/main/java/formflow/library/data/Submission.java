package formflow.library.data;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATED;

import formflow.library.inputs.AddressParts;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.stereotype.Component;

/**
 * A class representing what a submission of the form flow looks like in the database.
 *
 * <p>
 * This class also provides a few static functions to work with Submissions.
 * </p>
 */
@Slf4j
@Entity
@Table(name = "submissions")
@Getter
@Setter
@ToString
@AllArgsConstructor
@Component
@Builder
public class Submission {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "flow")
    private String flow;

    @Type(JsonType.class)
    @Column(name = "input_data", columnDefinition = "jsonb")
    private Map<String, Object> inputData;

    @Type(JsonType.class)
    @Column(name = "url_params", columnDefinition = "jsonb")
    private Map<String, String> urlParams;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "short_code")
    private String shortCode;

    /**
     * The key name for the field in an iteration's data that holds the status of completion for the iteration.
     */
    public static final String ITERATION_IS_COMPLETE_KEY = "iterationIsComplete";

    /**
     * Creates a new <code>Submission</code> with empty content
     */
    public Submission() {
        inputData = new HashMap<>();
        urlParams = new HashMap<>();
    }

    /**
     * Provides access to a specific subflow's submission data for a particular UUID, if the subflow is present in the
     * submission.
     *
     * <p>
     * If the subflow is present in the submission, this method will return the data matching the passed in UUID for that
     * subflow.
     * </p>
     *
     * <p>
     * If the subflow is not present in the submission or the UUID is not present in a set of the subflow's data, null will be
     * returned.
     * </p>
     *
     * @param subflowName subflow of interest, not null
     * @param uuid        the uuid of the data of interest in the subflow, not null
     * @return the requested subflow's set of data for the uuid, null if subflow not present
     */
    public Map<String, Object> getSubflowEntryByUuid(String subflowName, String uuid) {
        if (!inputData.containsKey(subflowName)) {
            return null;
        }

        List<Map<String, Object>> subflow = (List<Map<String, Object>>) inputData.get(subflowName);
        Optional<Map<String, Object>> iteration = subflow.stream().filter(entry -> entry.get("uuid").equals(uuid)).findFirst();

        return iteration.isPresent() ? iteration.get() : null;
    }

    /**
     * Merges the passed in data into the data stored in the Submission.
     *
     * <p>
     * The Submission will be updated with the merged data.
     * </p>
     *
     * @param formSubmission new data to be merged with data in the submission, may be empty but should not be null
     */
    public void mergeFormDataWithSubmissionData(FormSubmission formSubmission) {
        inputData.forEach((key, value) -> formSubmission.getFormData().merge(key, value, (newValue, oldValue) -> newValue));
        inputData = formSubmission.getFormData();
    }

    /**
     * Merges the passed in query parameters into the Submission's urlParams Map
     *
     * @param queryParams the Map of query parameters to merge in
     */
    public void mergeUrlParamsWithData(Map<String, String> queryParams) {
        urlParams.putAll(queryParams);
    }

    /**
     * Merges the passed in form submission data with the subflow's iteration data into the Submission's subflow's data for that
     * iteration.
     *
     * @param subflowName        subflow that the iteration data belongs with, not null
     * @param iterationToUpdate  existing data for a particular iteration of a subflow, may be empty, but not null
     * @param formDataSubmission new data for a particular iteration of a subflow, not null
     */
    public void mergeFormDataWithSubflowIterationData(String subflowName, Map<String, Object> iterationToUpdate,
            Map<String, Object> formDataSubmission) {

        iterationToUpdate.forEach((key, value) -> formDataSubmission.merge(key, value, (newValue, OldValue) -> newValue));
        var subflowArr = (List<Map<String, Object>>) inputData.get(subflowName);
        int indexToUpdate = subflowArr.indexOf(iterationToUpdate);
        subflowArr.set(indexToUpdate, formDataSubmission);
        inputData.replace(subflowName, subflowArr);
    }

    /**
     * Merges the passed in form submission data with the subflow's repeatFor iteration data into the Submission's subflow's
     * repeatsFor iteration data.
     *
     * @param subflowName            subflow that the iteration data belongs with, not null
     * @param subflowUuid            subflow iteration uuid
     * @param repeatForSaveDataAsKey the value of the repeatFor saveDataAs configuration
     * @param iterationToUpdate      existing data for a particular iteration of a subflow, may be empty, but not null
     * @param formDataSubmission     new data for a particular iteration of a subflow, not null
     */
    public void mergeFormDataWithRepeatForSubflowIterationData(String subflowName, String subflowUuid,
            String repeatForSaveDataAsKey, Map<String, Object> iterationToUpdate, Map<String, Object> formDataSubmission) {

        iterationToUpdate.forEach((key, value) -> formDataSubmission.merge(key, value, (newValue, OldValue) -> newValue));

        Map<String, Object> subflowEntry = getSubflowEntryByUuid(subflowName, subflowUuid);
        List<Map<String, Object>> repeatForIterations = (List<Map<String, Object>>) subflowEntry.getOrDefault(
                repeatForSaveDataAsKey, Collections.EMPTY_LIST);

        Optional<Map<String, Object>> currentIteration = repeatForIterations.stream()
                .filter(iteration -> iteration.get("uuid").equals(formDataSubmission.get("uuid"))).findFirst();

        if (currentIteration.isPresent()) {
            int nestedIndexToUpdate = repeatForIterations.indexOf(currentIteration.get());
            repeatForIterations.set(nestedIndexToUpdate, formDataSubmission);

            subflowEntry.put(repeatForSaveDataAsKey, repeatForIterations);

            List<Map<String, Object>> subflowArr = (List<Map<String, Object>>) this.inputData.get(subflowName);
            int indexToUpdate = subflowArr.indexOf(subflowEntry);
            subflowArr.set(indexToUpdate, subflowEntry);
            this.inputData.put(subflowName, subflowArr);
        }
    }

    /**
     * Sets the 'iterationIsComplete' status for a given iteration.
     *
     * @param subflow       the subflow the iteration is a part of
     * @param iterationUuid the uuid that identifies the iteration to update
     */
    public void setIterationIsCompleteToTrue(String subflow, String iterationUuid) {
        Map<String, Object> iterationData = getSubflowEntryByUuid(subflow, iterationUuid);
        if (iterationData == null) {
            log.warn("No iteration data found for subflow '{}' with UUID '{}'", subflow, iterationUuid);
            return;
        }
        iterationData.put(ITERATION_IS_COMPLETE_KEY, true);
    }

    /**
     * Returns the status of whether the given iteration is complete.
     *
     * @param subflow       the subflow the iteration is a part of
     * @param iterationUuid the iteration to find the completion status of
     * @return Boolean of true/false, maybe be null if the status is unset
     */
    public Boolean getIterationIsCompleteStatus(String subflow, String iterationUuid) {
        Map<String, Object> iterationData = getSubflowEntryByUuid(subflow, iterationUuid);
        if (iterationData == null) {
            log.warn("Iteration completion status request, but iteration with id '{}' not found.", iterationUuid);
            return null;
        }

        return (Boolean) iterationData.get(ITERATION_IS_COMPLETE_KEY);
    }

    /**
     * Removes the data in the inputData pertaining to the inputName of the address passed in.
     *
     * @param inputName name of the address field to clear the data of
     */
    public void clearAddressFields(String inputName) {
        // we want to clear out
        inputData.remove(inputName + AddressParts.STREET_ADDRESS_1 + UNVALIDATED_FIELD_MARKER_VALIDATED);
        inputData.remove(inputName + AddressParts.STREET_ADDRESS_2 + UNVALIDATED_FIELD_MARKER_VALIDATED);
        inputData.remove(inputName + AddressParts.CITY + UNVALIDATED_FIELD_MARKER_VALIDATED);
        inputData.remove(inputName + AddressParts.STATE + UNVALIDATED_FIELD_MARKER_VALIDATED);
        inputData.remove(inputName + AddressParts.ZIPCODE + UNVALIDATED_FIELD_MARKER_VALIDATED);
    }

    /**
     * Create a deep copy of the given submission.
     *
     * @param origSubmission given submission to copy
     * @return deep copy of origSubmission
     */
    public static Submission copySubmission(Submission origSubmission) {
        Submission newSubmission = new Submission();
        newSubmission.setUrlParams(new HashMap<>(origSubmission.getUrlParams()));

        newSubmission.setFlow(origSubmission.getFlow());
        newSubmission.setCreatedAt(origSubmission.getCreatedAt());
        newSubmission.setUpdatedAt(origSubmission.getUpdatedAt());
        newSubmission.setSubmittedAt(origSubmission.getSubmittedAt());
        newSubmission.setId(origSubmission.getId());

        newSubmission.setShortCode(origSubmission.getShortCode());

        // deep copy the subflows and any lists
        newSubmission.setInputData(copyMap(origSubmission.getInputData()));
        return newSubmission;
    }


    private static Map<String, Object> copyMap(Map<String, Object> origMap) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : origMap.entrySet()) {
            if (entry.getValue() instanceof List) {
                List data = (List) entry.getValue();
                if (data.size() == 0) {
                    result.put(entry.getKey(), new ArrayList<>());
                } else if (data.get(0) instanceof String) {
                    result.put(entry.getKey(), new ArrayList<>(data));
                } else if (data.get(0) instanceof Map) {
                    List<Map> newList = new ArrayList<>();
                    for (Map element : (List<Map>) data) {
                        newList.add(copyMap(element));
                    }
                    result.put(entry.getKey(), newList);
                } else {
                    result.put(entry.getKey(), entry.getValue());
                }
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void setShortCode(String shortCode) {
        if (this.shortCode != null) {
            throw new UnsupportedOperationException("Cannot change shortCode for an existing submission");
        }
        this.shortCode = shortCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Submission that = (Submission) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
