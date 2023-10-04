package formflow.library.framework;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

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

@SpringBootTest(properties = {"form-flow.path=flows-config/test-on-post-action.yaml"})
public class OnPostActionTest extends AbstractMockMvcTest {

  Submission submission;

  private MockMvc mockMvc;

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  @Autowired
  private ScreenController screenController;

  private Map<String, Object> sessionAttributes;

  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(screenController).build();
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).inputData(new HashMap<>()).build();

    sessionAttributes = Map.of(
        SUBMISSION_MAP_NAME,
        new HashMap<String, Object>(
            Map.of("testFlow", submission.getId())
        )
    );

    super.setUp();
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    when(submissionRepositoryService.save(any())).thenReturn(submission);
  }


  @Test
  void shouldSaveFormattedDataInNewFieldAndValidateSuccessfully() throws Exception {
    postExpectingSuccess("inputs",
        Map.of(
            "dateMonth", List.of("1"),
            "dateDay", List.of("3"),
            "dateYear", List.of("1999")),
        sessionAttributes
    );

    assertThat(submission.getInputData().get("dateFull")).isEqualTo("1/3/1999");
  }

  @Test
  void shouldSaveFormattedDateInNewFieldAndFailValidation() throws Exception {
    final String dateErrorMessage = "Date must be in the format of mm/dd/yyyy";
    postExpectingFailure("inputs",
        Map.of(
            "dateMonth", List.of("abc"),
            "dateDay", List.of("1"),
            "dateYear", List.of("1999")),
        sessionAttributes
    );

    assertPageHasInputError("inputs", "dateFull", dateErrorMessage);
  }
}

