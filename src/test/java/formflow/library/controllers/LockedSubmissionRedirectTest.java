package formflow.library.controllers;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.data.Submission;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
@TestPropertySource(properties = {
    "form-flow.lock-after-submitted[0].flow=testFlow",
    "form-flow.lock-after-submitted[0].submissionLockedRedirectPage=success"
})
public class LockedSubmissionRedirectTest extends AbstractMockMvcTest {

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    // this setups flow info in the session to get passed along later on.
    setFlowInfoInSession(session, "testFlow", submission.getId());
    super.setUp();
  }

  @Test
  public void shouldRedirectToConfiguredScreenWhenAFlowsSubmissionIsConfiguredToLockAfterSubmitted() throws Exception {
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
    
    ResultActions result = mockMvc.perform(post("/flow/testFlow/pageWithCustomSubmitButton")
        .session(session));
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
    Optional<Submission> testFlowSubmissionAfterBeingSubmitted = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterBeingSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterBeingSubmitted.get().getSubmittedAt()).isNotNull();
    
    // Assert that we are redirected to the configured screen when we try to go back into the flow when the submission should be locked
    mockMvc.perform(get("/flow/testFlow/inputs")
        .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success", Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));
  }
  
  @Test
  void shouldRedirectToConfiguredScreenWhenPostingDataToAFlowThatHasBeenConfiguredToLockAfterBeingSubmittedWithoutUpdatingSubmissionObject()
      throws Exception {
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

    ResultActions result = mockMvc.perform(post("/flow/testFlow/pageWithCustomSubmitButton")
        .session(session));
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
    Optional<Submission> testFlowSubmissionAfterBeingSubmitted = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterBeingSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterBeingSubmitted.get().getSubmittedAt()).isNotNull();
    
    // Post again and assert that the submission is not updated
    mockMvc.perform(post("/flow/testFlow/inputs")
        .session(session)
        .params(new LinkedMultiValueMap<>(Map.of(
            "textInput", List.of("newValue"),
            "numberInput", List.of("333"),
            "moneyInput", List.of("444"))))
    );

    Optional<Submission> testFlowSubmissionAfterAttemptingToPostAfterSubmitted = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getSubmittedAt()).isNotNull();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().get("textInput")).isEqualTo("firstFlowTextInputValue");
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().get("numberInput")).isEqualTo("10");
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().get("moneyInput")).isNull();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().equals(testFlowSubmissionAfterBeingSubmitted.get().getInputData())).isTrue();
  }
  
  // TODO create tests for GET and POST in subflows
  
}
