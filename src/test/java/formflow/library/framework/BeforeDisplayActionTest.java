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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-before-display-action.yaml"})
public class BeforeDisplayActionTest extends AbstractMockMvcTest {

  Submission submission;

  @MockitoBean
  private SubmissionRepositoryService submissionRepositoryService;

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).inputData(new HashMap<>()).build();
    setFlowInfoInSession(session, "testFlow", submission.getId());
    super.setUp();
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    when(submissionRepositoryService.save(any())).thenReturn(submission);
  }

  @Test
  void shouldSaveEncryptedSSN() throws Exception {
    // beforeSave
    String ssnInput = "111-00-1234";
    postExpectingSuccess("inputs", Map.of("ssnInput", List.of(ssnInput)));
    assertThat(submission.getInputData().get("ssnInputEncrypted")).isEqualTo("BBB-AA-BCDE");
    assertThat(submission.getInputData().get("ssnInput")).isNull();

    // beforeDisplay
    MvcResult result = getPageExpectingSuccess("testFlow", "inputs").andReturn();
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
        Submission.ITERATION_IS_COMPLETE_KEY, true));
    subflowList.add(Map.of("uuid", UUID.randomUUID().toString(),
        "ssnInput", "222-22-2222",
        Submission.ITERATION_IS_COMPLETE_KEY, true));

    submission.getInputData().put("householdMembers", subflowList);

    // beforeSave
    String ssnInput = "333-33-3333";
    String navigationUrl = "/flow/testFlow/pageWithSSNInput/navigation?uuid=" + subflowUuid;
    postToUrlExpectingSuccess("/flow/testFlow/pageWithSSNInput", navigationUrl,
        Map.of("ssnInput", List.of(ssnInput)), subflowUuid);
    assertThat(followRedirectsForUrl(navigationUrl)).isEqualTo("/flow/testFlow/subflowReview");

    Map<String, Object> subflowEntry = submission.getSubflowEntryByUuid("householdMembers",
        subflowUuid);
    assertThat(subflowEntry.get("ssnInputEncrypted")).isEqualTo("DDD-DD-DDDD");
    assertThat(subflowEntry.get("ssnInput")).isNull();

    // beforeDisplay
    MvcResult result = getPageExpectingSuccess("testFlow", "pageWithSSNInput/" + subflowUuid + "/edit").andReturn();
    Map<String, String> subflowItem = (Map<String, String>) result.getModelAndView().getModel().get("currentSubflowItem");
    assertThat(subflowItem.get("ssnInput")).isEqualTo(ssnInput);
    assertThat(subflowItem.get("ssnInputEncrypted")).isNull();
    subflowEntry = submission.getSubflowEntryByUuid("householdMembers", subflowUuid);
    assertThat(subflowEntry.get("ssnInputEncrypted")).isNull();
    assertThat(subflowEntry.get("ssnInput")).isEqualTo(ssnInput);
  }

}
