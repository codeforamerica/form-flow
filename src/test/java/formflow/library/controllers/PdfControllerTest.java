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
import formflow.library.PdfService;
import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class PdfControllerTest extends AbstractMockMvcTest {

  private Submission submission;
  private MockMvc mockMvc;
  private final PdfService pdfService = mock(PdfService.class);
  private String flow;
  private byte[] filledPdfByteArray;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    flow = "ubi";
    PdfController pdfController = new PdfController(messageSource, pdfService);
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder()
        .id(UUID.randomUUID())
        .flow(flow)
        .build();

    filledPdfByteArray = new byte[20];
    when(pdfService.getFilledOutPDF(flow, submission.getId().toString())).thenReturn(filledPdfByteArray);
    super.setUp();
  }

  @Test
  public void getDownloadGeneratesAndReturnsFilledFlattenedPdf() throws Exception {
    session.setAttribute("id", submission.getId());
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=%s.pdf".formatted(pdfService.generatePdfName(flow, submission.getId().toString()))))
        .andExpect(status().is2xxSuccessful())
        .andReturn();

    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdfByteArray);

    verify(pdfService, times(1)).getFilledOutPDF(flow, submission.getId().toString());
  }

  @Test
  public void shouldNotAllowDownloadingAPdfWithADifferentSubmissionIdThanTheActiveSession() throws Exception {
    session.setAttribute("id", UUID.randomUUID());

    mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
        .andExpect(status().is4xxClientError());

    session.setAttribute("id", submission.getId());

    mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
        .andExpect(status().is2xxSuccessful());
  }
}
