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

  public static Map<String, Object> getSubflowEntryByUuid(String subflowName, String uuid, Submission submission) {
    if (submission.getInputData().containsKey(subflowName)) {
      ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
      return subflow.stream().filter(entry -> entry.get("uuid").equals(uuid)).toList().get(0);
    }
    return null;
  }

  public static void mergeFormDataWithSubmissionData(Submission submission, Map<String, Object> formDataSubmission) {
    Map<String, Object> inputData = submission.getInputData();
    inputData.forEach((key, value) -> formDataSubmission.merge(key, value, (newValue, oldValue) -> newValue));
    submission.setInputData(formDataSubmission);
  }

  public static Submission mergeFormDataWithSubflowIterationData(Submission submission, String subflowName, Map<String, Object> iterationToUpdate, Map<String, Object> formDataSubmission) {
    iterationToUpdate.forEach((key, value) -> formDataSubmission.merge(key, value, (newValue, OldValue) -> newValue));
    var subflowArr = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
    var existingInputData = submission.getInputData();
    int indexToUpdate = subflowArr.indexOf(iterationToUpdate);
    subflowArr.set(indexToUpdate, formDataSubmission);
    existingInputData.replace(subflowName, subflowArr);
    submission.setInputData(existingInputData);
    return submission;
  }

  public static void removeIncompleteIterations(Submission submission, String subflowName, String uuid) {
    List<Map<String, Object>> toRemove = new ArrayList<>();
    ArrayList<Map<String, Object>> subflow = (ArrayList<Map<String, Object>>) submission.getInputData()
        .get(subflowName);
    subflow.forEach(iteration -> {
      if (iteration.get("iterationIsComplete").equals(false) && !iteration.get("uuid")
          .equals(uuid)) {
        toRemove.add(iteration);
      }
    });
    subflow.removeAll(toRemove);
  }
}
