package formflow.library.data;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.*;

import static javax.persistence.TemporalType.TIMESTAMP;

/**
 * A class representing what a submission of the form flow looks like in the database.
 *
 * <p>
 * This class also provides a few static functions to work with Submissions.
 * </p>
 */
@TypeDef(
    name = "json", typeClass = JsonType.class
)
@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@Builder
public class Submission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "flow")
  private String flow;

  @Type(type = "json")
  @Column(name = "input_data", columnDefinition = "jsonb")
  private Map<String, Object> inputData = new HashMap<>();

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
   * @param submission  submission to look for the data in, not null
   * @return the requested subflow's set of data for the uuid, null if subflow not present
   */
  public static Map<String, Object> getSubflowEntryByUuid(String subflowName, String uuid, Submission submission) {
    if (submission.getInputData().containsKey(subflowName)) {
      ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
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
   * @param submission         existing Submission to be updated, not null
   * @param formDataSubmission new data to be merged with data in the submission, may be empty but should not be null
   */
  public static void mergeFormDataWithSubmissionData(Submission submission, Map<String, Object> formDataSubmission) {
    Map<String, Object> inputData = submission.getInputData();
    inputData.forEach((key, value) -> formDataSubmission.merge(key, value, (newValue, oldValue) -> newValue));
    submission.setInputData(formDataSubmission);
  }

  /**
   * Merges the passed in subflow's iteration data into the Submission's subflow's data for that iteration.
   *
   * @param submission         existing Submission to be updated, not null
   * @param subflowName        subflow that the iteration data belongs with, not null
   * @param iterationToUpdate  existing data for a particular iteration of a subflow, may be empty, but not null
   * @param formDataSubmission new data for a particular iteration of a subflow, not null
   * @return updated submission object
   */
  public static Submission mergeFormDataWithSubflowIterationData(Submission submission, String subflowName,
      Map<String, Object> iterationToUpdate, Map<String, Object> formDataSubmission) {
    iterationToUpdate.forEach((key, value) -> formDataSubmission.merge(key, value, (newValue, OldValue) -> newValue));
    var subflowArr = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
    var existingInputData = submission.getInputData();
    int indexToUpdate = subflowArr.indexOf(iterationToUpdate);
    subflowArr.set(indexToUpdate, formDataSubmission);
    existingInputData.replace(subflowName, subflowArr);
    submission.setInputData(existingInputData);
    return submission;
  }

  /**
   * Removes any data in an interation that has "iterationIsComplete" set to "false" and has a {@code uuid} not equal to the
   * {@code currentUuid} provided.
   *
   * @param submission  existing submission we are checking the iteration data in, not null
   * @param subflowName subflow that we are checking the iteration data for, not null
   * @param currentUuid The current uuid being worked on, not null. The data associated with this uuid will not be deleted.
   */
  public static void removeIncompleteIterations(Submission submission, String subflowName, String currentUuid) {
    List<Map<String, Object>> toRemove = new ArrayList<>();
    ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
    subflow.forEach(iteration -> {
      if (iteration.get("iterationIsComplete").equals(false) && !iteration.get("uuid").equals(currentUuid)) {
        toRemove.add(iteration);
      }
    });
    subflow.removeAll(toRemove);
  }
}
