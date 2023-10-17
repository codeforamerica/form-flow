package formflow.library.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubmissionTests {

  private Submission submission;
  private final String iterationUuid = UUID.randomUUID().toString();

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
}
