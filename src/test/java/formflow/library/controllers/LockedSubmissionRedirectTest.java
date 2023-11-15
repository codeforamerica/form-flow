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
    "form-flow.lock-after-submitted[0].redirect=success"
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
  public void shouldRedirectWhenAttemptingToGetAPageThatIsNotAllowedForAFlowWithALockedSubmission() throws Exception {
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
            "moneyInput", List.of("444")))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success", Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));

    Optional<Submission> testFlowSubmissionAfterAttemptingToPostAfterSubmitted = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getSubmittedAt()).isNotNull();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().get("textInput")).isEqualTo("firstFlowTextInputValue");
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().get("numberInput")).isEqualTo("10");
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().get("moneyInput")).isNull();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData().equals(testFlowSubmissionAfterBeingSubmitted.get().getInputData())).isTrue();
  }
  
  @Test
  void shouldRedirectWhenAttemptingToGetAScreenInsideASubflowThatIsNotAllowedForAFlowWithALockedSubmission() throws Exception {
    // Make an initial post to create the submission and give it some data
    mockMvc.perform(post("/flow/testFlow/subflowAddItem/new")
        .session(session)
        .params(new LinkedMultiValueMap<>(Map.of(
            "textInputSubflow", List.of("textInputValue"),
            "numberInputSubflow", List.of("10"))))
    );
    
    // Get the UUID for the iteration we just created
    UUID testSubflowLogicUUID = ((Map<String, UUID>) session.getAttribute(SUBMISSION_MAP_NAME)).get("testFlow");
    Submission submissionBeforeSubflowIsCompleted = submissionRepositoryService.findById(testSubflowLogicUUID).get();
    List<Map<String, Object>> subflowIterations = (List<Map<String, Object>>) submissionBeforeSubflowIsCompleted.getInputData()
        .get("testSubflow");
    String uuidString = (String) subflowIterations.get(0).get("uuid");

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
    mockMvc.perform(get("/flow/testFlow/subflowAddItem/" + uuidString )
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success", Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));
    
    // Assert the same as above but for the /edit endpoint
    mockMvc.perform(get("/flow/testFlow/subflowAddItem/" + uuidString + "/edit")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success", Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));
  }
  
  @Test
  void shouldRedirectToConfiguredScreenWhenPostingSubflowDataToAFlowThatHasBeenConfiguredToLockAfterBeingSubmittedWithoutUpdatingSubmissionObject()
      throws Exception {
    // Make an initial post to create the submission and give it some data
    mockMvc.perform(post("/flow/testFlow/subflowAddItem/new")
        .session(session)
        .params(new LinkedMultiValueMap<>(Map.of(
            "textInputSubflow", List.of("textInputValue"),
            "numberInputSubflow", List.of("10"))))
    );

    // Get the UUID for the iteration we just created
    UUID testSubflowLogicUUID = ((Map<String, UUID>) session.getAttribute(SUBMISSION_MAP_NAME)).get("testFlow");
    Submission submissionBeforeSubflowIsCompleted = submissionRepositoryService.findById(testSubflowLogicUUID).get();
    List<Map<String, Object>> subflowIterationsBeforeSubmit = (List<Map<String, Object>>) submissionBeforeSubflowIsCompleted.getInputData()
        .get("testSubflow");
    String uuidString = (String) subflowIterationsBeforeSubmit.get(0).get("uuid");

    // Assert that the submissions submittedAt value is null before submitting
    Map<String, UUID> submissionMap = (Map) session.getAttribute(SUBMISSION_MAP_NAME);
    Optional<Submission> testFlowSubmission = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmission.isPresent()).isTrue();
    assertThat(testFlowSubmission.get().getSubmittedAt()).isNull();
    
    // Submit the flow, assert we reach the success page
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
    
    // Post again and assert that the submission is not updated and that we are redirected
    mockMvc.perform(post("/flow/testFlow/subflowAddItem/" + uuidString)
            .session(session)
            .params(new LinkedMultiValueMap<>(Map.of(
                "textInputSubflow", List.of("newValue"),
                "numberInputSubflow", List.of("333"),
                "moneyInputSubflow", List.of("444")))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success", Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));

    Optional<Submission> testFlowSubmissionAfterAttemptingToPostAfterSubmitted = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getSubmittedAt()).isNotNull();

    List<Map<String, Object>> subflowIterationsAfterSubmit = (List<Map<String, Object>>) testFlowSubmissionAfterAttemptingToPostAfterSubmitted.get().getInputData()
        .get("testSubflow");
    Map<String, Object> subflowIteration = subflowIterationsAfterSubmit.get(0);
    assertThat(subflowIteration.get("textInputSubflow")).isEqualTo("textInputValue");
    assertThat(subflowIteration.get("numberInputSubflow")).isEqualTo("10");
    assertThat(subflowIteration.get("moneyInputSubflow")).isNull();
    assertThat(subflowIterationsAfterSubmit.equals(subflowIterationsBeforeSubmit)).isTrue();
    
    // Do the same but for the /edit endpoint
    mockMvc.perform(post("/flow/testFlow/subflowAddItem/" + uuidString + "/edit")
            .session(session)
            .params(new LinkedMultiValueMap<>(Map.of(
                "textInputSubflow", List.of("editValue"),
                "numberInputSubflow", List.of("111"),
                "moneyInputSubflow", List.of("222")))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirect -> assertEquals("/flow/testFlow/success", Objects.requireNonNull(redirect.getResponse().getRedirectedUrl())));

    Optional<Submission> testFlowSubmissionAfterAttemptingToEditAfterSubmitted = submissionRepositoryService.findById(submissionMap.get("testFlow"));
    assertThat(testFlowSubmissionAfterAttemptingToEditAfterSubmitted.isPresent()).isTrue();
    assertThat(testFlowSubmissionAfterAttemptingToEditAfterSubmitted.get().getSubmittedAt()).isNotNull();
    List<Map<String, Object>> subflowIterationsAfterEdit = (List<Map<String, Object>>) testFlowSubmissionAfterAttemptingToEditAfterSubmitted.get().getInputData()
        .get("testSubflow");
    Map<String, Object> subflowIterationAfterEdit = subflowIterationsAfterEdit.get(0);
    assertThat(subflowIterationAfterEdit.get("textInputSubflow")).isEqualTo("textInputValue");
    assertThat(subflowIterationAfterEdit.get("numberInputSubflow")).isEqualTo("10");
    assertThat(subflowIterationAfterEdit.get("moneyInputSubflow")).isNull();
    assertThat(subflowIterationsAfterEdit.equals(subflowIterationsBeforeSubmit)).isTrue();
  }
}
