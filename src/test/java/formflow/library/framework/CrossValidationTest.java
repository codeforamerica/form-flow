package formflow.library.framework;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import formflow.library.ScreenController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-cross-validation-action.yaml"})
public class CrossValidationTest extends AbstractMockMvcTest {

  Submission submission;

  private MockMvc mockMvc;

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  @Autowired
  private ScreenController screenController;

  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    mockMvc = MockMvcBuilders.standaloneSetup(screenController).build();
    submission = Submission.builder().id(submissionUUID).inputData(new HashMap<>()).build();

    super.setUp();
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
  }

  @Test
  void shouldAcceptEmailWithPreference() throws Exception {
    postExpectingSuccess("contactInfoPreference",
        Map.of(
            "emailAddress", List.of("foo@bar.com"),
            "contactMethod", List.of("emailPreferred")));
  }

  @Test
  void shouldDisplayFieldAndCrossValidationMessages() throws Exception {
    final String emailErrorMessage = "please enter a valid email";
    postExpectingFailure("contactInfoPreference",
        Map.of("emailAddress", List.of("malformed.com"), "contactMethod", List.of("emailPreferred")));
    assertPageHasInputError("contactInfoPreference", "emailAddress", emailErrorMessage);

  }

  @Test
  void shouldAcceptPhoneNumberWithPreference() throws Exception {
    postExpectingSuccess("contactInfoPreference",
        Map.of(
            "phoneNumber", List.of("123-456-7891"),
            "contactMethod", List.of("phonePreferred")));
  }

  @Test
  void shouldFailWithPhoneNumberPreferenceNoPhone() throws Exception {
    final String phoneErrorMessage = "please provide a phone number";
    postExpectingFailure("contactInfoPreference",
        Map.of(
            "contactMethod", List.of("phonePreferred"),
            "phoneNumber", List.of("")));

    assertPageHasInputError("contactInfoPreference", "phoneNumber", phoneErrorMessage);
  }

  @Test
  void shouldFailWithEmailPreferenceNoEmail() throws Exception {
    final String emailErrorMessage = "please provide an email address";
    postExpectingFailure("contactInfoPreference",
        Map.of(
            "contactMethod", List.of("emailPreferred"),
            "emailAddress", List.of("")));

    assertPageHasInputError("contactInfoPreference", "emailAddress", emailErrorMessage);
  }
}
