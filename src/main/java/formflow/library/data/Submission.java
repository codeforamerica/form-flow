package formflow.library.data;

import static jakarta.persistence.TemporalType.TIMESTAMP;

import formflow.library.inputs.AddressParts;
import formflow.library.inputs.UnvalidatedField;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
  @Temporal(TIMESTAMP)
  @Column(name = "created_at")
  private Date createdAt;

  @UpdateTimestamp
  @Temporal(TIMESTAMP)
  @Column(name = "updated_at")
  private Date updatedAt;

  @Temporal(TIMESTAMP)
  @Column(name = "submitted_at")
  private Date submittedAt;

  public Submission() {
    inputData = new HashMap<>();
    urlParams = new HashMap<>();
  }

  /**
   * Provides access to a specific subflow's submission data for a particular UUID, if the subflow is present in the submission.
   *
   * <p>
   * If the subflow is present in the submission, this method will return the data matching the passed in UUID for that subflow.
   * </p>
   *
   * <p>
   * If the subflow is not present in the submission or the UUID is not present in a set of the subflow's data, null will be
   * returned.
   *     TODO this will actually throw an exception, it seems, if the subflow is found, but the UUID is not in it - maybe that's okay?
   * </p>
   *
   * @param subflowName subflow of interest, not null
   * @param uuid        the uuid of the data of interest in the subflow, not null
   * @return the requested subflow's set of data for the uuid, null if subflow not present
   */
  public Map<String, Object> getSubflowEntryByUuid(String subflowName, String uuid) {
    if (inputData.containsKey(subflowName)) {
      List<Map<String, Object>> subflow = (List<Map<String, Object>>) inputData.get(subflowName);
      return subflow.stream().filter(entry -> entry.get("uuid").equals(uuid)).toList().get(0);
    }
    return null;
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

  public void mergeUrlParamsWithData(Map<String, String> passedParams) {
    urlParams.putAll(passedParams);
  }

  /**
   * Merges the passed in subflow's iteration data into the Submission's subflow's data for that iteration.
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
   * Removes any data in an interation that has "iterationIsComplete" set to "false" and has a {@code uuid} not equal to the
   * {@code currentUuid} provided.
   *
   * @param subflowName subflow that we are checking the iteration data for, not null
   * @param currentUuid The current uuid being worked on, not null. The data associated with this uuid will not be deleted.
   */
  public void removeIncompleteIterations(String subflowName, String currentUuid) {
    List<Map<String, Object>> toRemove = new ArrayList<>();
    List<Map<String, Object>> subflow = (List<Map<String, Object>>) inputData.get(subflowName);
    subflow.forEach(iteration -> {
      if (iteration.get("iterationIsComplete").equals(false) && !iteration.get("uuid").equals(currentUuid)) {
        toRemove.add(iteration);
      }
    });
    subflow.removeAll(toRemove);
  }

  /**
   * Removes the data in the inputData pertaining to the inputName of the address passed in.
   *
   * @param inputName name of the address field to clear the data of
   */
  public void clearAddressFields(String inputName) {
    // we want to clear out
    inputData.remove(inputName + AddressParts.STREET_ADDRESS_1 + UnvalidatedField.VALIDATED);
    inputData.remove(inputName + AddressParts.STREET_ADDRESS_2 + UnvalidatedField.VALIDATED);
    inputData.remove(inputName + AddressParts.CITY + UnvalidatedField.VALIDATED);
    inputData.remove(inputName + AddressParts.STATE + UnvalidatedField.VALIDATED);
    inputData.remove(inputName + AddressParts.ZIPCODE + UnvalidatedField.VALIDATED);
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
