package formflow.library.controllers;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.data.Submission;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
@TestPropertySource(properties = {
    "form-flow.lock-after-submitted[0].flow=testFlow",
    "form-flow.lock-after-submitted[0].redirect=success",
    "form-flow.short-code.creation-point=creation"
})
public class ScreenControllerShortCodeCreationPointTest extends AbstractMockMvcTest {

  @MockitoBean
  private UserFileRepositoryService userFileRepositoryService;

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    // this sets up flow info in the session to get passed along later on.
    setFlowInfoInSession(session, "testFlow", submission.getId());
    super.setUp();
  }

  @Test
  public void testSubmissionWithShortCodeCreatedAtCreation() throws Exception {
    // Make an initial post to create the submission and give it some data
    mockMvc.perform(post("/flow/testFlow/inputs")
        .session(session)
        .params(new LinkedMultiValueMap<>(Map.of(
            "textInput", List.of("firstFlowTextInputValue"),
            "numberInput", List.of("10"))))
    );

    // Assert that the submissions submittedAt value is null before submitting
    Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);
    Optional<Submission> testFlowSubmission = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmission.isPresent()).isTrue();
    assertThat(testFlowSubmission.get().getSubmittedAt()).isNull();
//    assertThat(testFlowSubmission.get().getShortCode()).isNotNull();

    ResultActions result = mockMvc.perform(post("/flow/testFlow/pageWithCustomSubmitButton/submit").session(session));
    String nextScreenUrl = "/flow/testFlow/pageWithCustomSubmitButton/navigation";
    result.andExpect(redirectedUrl(nextScreenUrl));

    while (Objects.requireNonNull(nextScreenUrl).contains("/navigation")) {
      // follow redirects
      nextScreenUrl = mockMvc.perform(get(nextScreenUrl).session(session))
          .andExpect(status().is3xxRedirection()).andReturn()
          .getResponse()
          .getRedirectedUrl();
    }
    assertThat(nextScreenUrl).isEqualTo("/flow/testFlow/success");
    FormScreen nextScreen = new FormScreen(mockMvc.perform(get(nextScreenUrl)));
    assertThat(nextScreen.getTitle()).isEqualTo("Success");

    // Assert that the submissions submittedAt value is not null after submitting
    Optional<Submission> testFlowSubmissionAfterBeingSubmitted = submissionRepositoryService.findById(
        submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterBeingSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterBeingSubmitted.get().getSubmittedAt()).isNotNull();
    assertThat(testFlowSubmissionAfterBeingSubmitted.get().getShortCode()).isNotNull();


    // Assert that we are redirected to the configured screen when we try to go back into the flow when the submission should be locked
    mockMvc.perform(get("/flow/testFlow/inputs")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success",
            Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));
  }
}
