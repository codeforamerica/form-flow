package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.PdfController;
import formflow.library.config.FlowConfiguration;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.pdf.PdfService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class PdfControllerTest extends AbstractMockMvcTest {

  private Submission submission;
  private MockMvc mockMvc;
  private final PdfService pdfService = mock(PdfService.class);

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  @MockBean
  private UserFileRepositoryService userFileRepositoryService;
  private byte[] filledPdfByteArray;

  private final String flowName = "testFlow";
  private final String otherFlowName = "otherTestFlow";

  private final UUID submissionId = UUID.randomUUID();

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    FlowConfiguration flowConfiguration = new FlowConfiguration();
    FlowConfiguration flowConfigurationOther = new FlowConfiguration();
    flowConfiguration.setName(flowName);
    flowConfigurationOther.setName(otherFlowName);
    List<FlowConfiguration> flowConfigurations = List.of(
        flowConfiguration, flowConfigurationOther
    );

    PdfController pdfController = new PdfController(messageSource, pdfService, submissionRepositoryService,
        userFileRepositoryService, flowConfigurations);
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();

    submission = Submission.builder()
        .id(submissionId)
        .flow(flowName)
        .build();
    filledPdfByteArray = new byte[20];

    setFlowInfoInSession(session,
        flowName, submission.getId()
    );

    when(pdfService.getFilledOutPDF(submission)).thenReturn(filledPdfByteArray);
    when(submissionRepositoryService.findById(submissionId)).thenReturn(Optional.of(submission));
    super.setUp();
  }

  @Test
  void shouldReturn404WhenFlowDoesNotExist() throws Exception {
    getPdfFile(submission, "flowThatDoesNotExist", status().isNotFound(), false);
  }

  @Test
  public void getDownloadGeneratesAndReturnsFilledFlattenedPdf() throws Exception {
    MvcResult result = getPdfFile(submission, flowName, status().is2xxSuccessful(), true);
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdfByteArray);
    verify(pdfService, times(1)).getFilledOutPDF(submission);
  }

  @Test
  public void shouldNotAllowDownloadingAPdfWithADifferentSubmissionIdThanTheActiveSession() throws Exception {
    // first test with bogus id
    setFlowInfoInSession(session, flowName, UUID.randomUUID());
    getPdfFile(submission, flowName, status().is4xxClientError(), false);

    // now test with legitimate id
    setFlowInfoInSession(session, flowName, submission.getId());
    getPdfFile(submission, flowName, status().is2xxSuccessful(), false);
  }

  @Test
  public void shouldReturnCorrectFileWhenMultipleFlowsExist() throws Exception {
    UUID otherSubmissionId = UUID.randomUUID();
    Submission otherSubmission = Submission.builder()
        .id(otherSubmissionId)
        .flow(otherFlowName)
        .build();
    byte[] otherByteArray = new byte[45];

    setFlowInfoInSession(session,
        flowName, submission.getId(),
        otherFlowName, otherSubmission.getId()
    );

    when(pdfService.getFilledOutPDF(otherSubmission)).thenReturn(otherByteArray);
    when(submissionRepositoryService.findById(otherSubmissionId)).thenReturn(Optional.of(otherSubmission));

    MvcResult result = getPdfFile(submission, flowName, status().is2xxSuccessful(), true);
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdfByteArray);

    MvcResult otherResult = getPdfFile(otherSubmission, otherFlowName, status().is2xxSuccessful(), true);
    assertThat(otherResult.getResponse().getContentAsByteArray()).isEqualTo(otherByteArray);

    verify(pdfService, times(1)).getFilledOutPDF(submission);
  }

  private MvcResult getPdfFile(Submission testSubmission, String testFlow, ResultMatcher resultMatcher, boolean expectsFile)
      throws Exception {
    ResultActions resultActions = mockMvc.perform(
            get("/download/" + testFlow + "/" + testSubmission.getId())
                .session(session)
        )
        .andExpect(resultMatcher);

    if (expectsFile) {
      resultActions.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=%s".formatted(pdfService.generatePdfName(testSubmission))));
    }

    return resultActions.andReturn();
  }
}
