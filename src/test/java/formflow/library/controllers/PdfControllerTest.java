package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    String flow = "ubi";
    FlowConfiguration flowConfiguration = new FlowConfiguration();
    flowConfiguration.setName(flow);
    List<FlowConfiguration> flowConfigurations = List.of(flowConfiguration);
    PdfController pdfController = new PdfController(messageSource, pdfService, submissionRepositoryService, userFileRepositoryService, flowConfigurations);
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder()
        .id(UUID.randomUUID())
        .flow(flow)
        .build();
    filledPdfByteArray = new byte[20];
    when(pdfService.getFilledOutPDF(submission)).thenReturn(filledPdfByteArray);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    super.setUp();
  }

  @Test
  void shouldReturn404WhenFlowDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/download/{flow}/{submissionId}", "flowThatDoesNotExist", "submissionId"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getDownloadGeneratesAndReturnsFilledFlattenedPdf() throws Exception {
    session.setAttribute("id", submission.getId());
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=%s".formatted(pdfService.generatePdfName(submission))))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdfByteArray);

    verify(pdfService, times(1)).getFilledOutPDF(submission);
  }

  @Test
  void shouldNotAllowDownloadingAPdfWithADifferentSubmissionIdThanTheActiveSession() throws Exception {
    session.setAttribute("id", UUID.randomUUID());

    mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
        .andExpect(status().is4xxClientError());

    session.setAttribute("id", submission.getId());

    mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
        .andExpect(status().is2xxSuccessful());
  }
}
