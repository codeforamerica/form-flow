package formflow.library.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

public class SubmissionTests {

  private Submission submission;
  private final String iterationUuid = UUID.randomUUID().toString();
  private Map<String, Object> repeatForEntry1 = new HashMap<>();

  @BeforeEach
  public void setUp() {
    Map<String, Object> inputData = new HashMap<>();
    ArrayList<Map<String, Object>> subflowArr = new ArrayList<>();

    Map<String, Object> subflowMap = new HashMap<>();
    subflowMap.put("_csrf", "a1fd9167-9d6d-4298-b9x4-2fc6c75ff3ab");
    subflowMap.put("firstName", "Rosie");
    subflowMap.put("uuid", iterationUuid);

    subflowArr.add(subflowMap);
    inputData.put("household", subflowArr);
    
    ArrayList<Map<String, Object>> repeatForSubflowArr = new ArrayList<>();
    Map<String, Object> repeatForSubflowMap = new HashMap<>();
    repeatForSubflowMap.put("uuid", "outer-repeat-for-uuid-1");
    repeatForSubflowMap.put("relatedId", "related-id-1");
    ArrayList<Map<String, Object>> repeatForArr = new ArrayList<>();
    repeatForEntry1 = new HashMap<>();
    repeatForEntry1.put("uuid", "inner-repeat-for-uuid-1");
    repeatForEntry1.put("foo", "bar");
    repeatForEntry1.put("iterationIsComplete", true);
    repeatForArr.add(repeatForEntry1);
    repeatForSubflowMap.put("saveDataAsName", repeatForArr);
    repeatForSubflowArr.add(repeatForSubflowMap);
    inputData.put("repeatForSubflow", repeatForSubflowArr);
    
    submission = Submission.builder()
        .inputData(inputData)
        .flow("testFlow")
        .build();
  }

  @Test
  public void shouldMarkSubmissionIterationComplete() {
    submission.setIterationIsCompleteToTrue("household", iterationUuid);
    Map<String, Object> subflowData = submission.getSubflowEntryByUuid("household", iterationUuid);
    assertThat(subflowData.containsKey(Submission.ITERATION_IS_COMPLETE_KEY)).isTrue();
    assertThat(subflowData.get(Submission.ITERATION_IS_COMPLETE_KEY)).isEqualTo(true);
  }
  
  @Test
  public void shouldRemoveCSRFFromRepeatForSubflowIterations() {
    Map<String, Object> formData = new HashMap<>();
    formData.put("_csrf", "csrf-token-value-should-be-removed");
    formData.put("keepMe", "keep-me-value");
    submission.mergeFormDataWithRepeatForSubflowIterationData("repeatForSubflow",
            "outer-repeat-for-uuid-1", "saveDataAsName", repeatForEntry1, formData);
    List<Map<String, Object>> repeatForSubflow = (List<Map<String, Object>>) submission.getInputData().get("repeatForSubflow");
    List<Map<String, Object>> saveDataAsName = (List<Map<String, Object>>) repeatForSubflow.get(0).get("saveDataAsName");
    assertThat(saveDataAsName.getFirst().containsKey("_csrf")).isFalse();
    assertThat(saveDataAsName.getFirst().get("keepMe")).isEqualTo("keep-me-value");
    assertThat(saveDataAsName.getFirst().get("foo")).isEqualTo("bar");
  }

}
