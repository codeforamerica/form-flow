package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-before-display-action.yaml"})
public class BeforeDisplayActionTest extends AbstractMockMvcTest {

  Submission submission;

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).inputData(new HashMap<>()).build();

    super.setUp();
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
  }

  @Test
  void shouldSaveEncryptedSSN() throws Exception {
    // beforeSave
    String ssnInput = "111-00-1234";
    postExpectingSuccess("inputs",
        Map.of("ssnInput", List.of(ssnInput)));
    assertThat(submission.getInputData().get("ssnInputEncrypted")).isEqualTo("BBB-AA-BCDE");
    assertThat(submission.getInputData().get("ssnInput")).isNull();

    // beforeDisplay
    MvcResult result = getPageExpectingSuccess("inputs").andReturn();
    Map<String, String> inputData = (Map<String, String>) result.getModelAndView().getModel().get("inputData");
    assertThat(inputData.get("ssnInput")).isEqualTo(ssnInput);
    assertThat(inputData.get("ssnInputEncrypted")).isNull();
    assertThat(submission.getInputData().get("ssnInputEncrypted")).isNull();
    assertThat(submission.getInputData().get("ssnInput")).isEqualTo(ssnInput);
  }

  @Test
  void shouldSaveEncryptedSSNInSubflow() throws Exception {
    String subflowUuid = UUID.randomUUID().toString();

    List<Map<String, Object>> subflowList = new ArrayList<>();
    subflowList.add(Map.of("uuid", subflowUuid));
    subflowList.add(Map.of("uuid", UUID.randomUUID().toString(),
        "ssnInput", "111-11-1111",
        "iterationIsComplete", true));
    subflowList.add(Map.of("uuid", UUID.randomUUID().toString(),
        "ssnInput", "222-22-2222",
        "iterationIsComplete", true));

    submission.getInputData().put("householdMembers", subflowList);

    // beforeSave
    String ssnInput = "333-33-3333";
    postToUrlExpectingSuccess("/flow/testFlow/pageWithSSNInput", "/flow/testFlow/subflowReview",
        Map.of("ssnInput", List.of(ssnInput)), subflowUuid);

    Map<String, Object> subflowEntry = submission.getSubflowEntryByUuid("householdMembers",
        subflowUuid);
    assertThat(subflowEntry.get("ssnInputEncrypted")).isEqualTo("DDD-DD-DDDD");
    assertThat(subflowEntry.get("ssnInput")).isNull();

    // beforeDisplay
    MvcResult result = getPageExpectingSuccess("pageWithSSNInput/" + subflowUuid + "/edit").andReturn();
    Map<String, String> subflowItem = (Map<String, String>) result.getModelAndView().getModel().get("currentSubflowItem");
    assertThat(subflowItem.get("ssnInput")).isEqualTo(ssnInput);
    assertThat(subflowItem.get("ssnInputEncrypted")).isNull();
    subflowEntry = submission.getSubflowEntryByUuid("householdMembers", subflowUuid);
    assertThat(subflowEntry.get("ssnInputEncrypted")).isNull();
    assertThat(subflowEntry.get("ssnInput")).isEqualTo(ssnInput);
  }

}
