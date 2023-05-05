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
import formflow.library.data.Submission;
import formflow.library.pdf.PdfFile;
import formflow.library.pdf.PdfGenerator;
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
  private PdfController pdfController;
  private PdfGenerator pdfGenerator = mock(PdfGenerator.class);
  private String testPdf;
  private PdfFile filledPdf;
  private String flow;


  @Override
  @BeforeEach
  public void setUp() throws Exception {
    testPdf = "testFile";
    flow = "ubi";
    pdfController = new PdfController(pdfGenerator);
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder().id(UUID.randomUUID()).build();
    filledPdf = mock(PdfFile.class);
    when(filledPdf.fileBytes()).thenReturn(new byte[5]);
    when(pdfGenerator.generate(flow, submission.getId())).thenReturn(filledPdf);
    super.setUp();
  }

  @Test
  public void getDownloadGeneratesAndReturnsFilledFlattenedPdf() throws Exception {
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId()))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=%s-%s.pdf".formatted(filledPdf.path(), submission.getId())))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdf.fileBytes());

    verify(pdfGenerator, times(1)).generate(flow, submission.getId());
    verify(filledPdf, times(1)).finalizeForSending();
  }
}
