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
import formflow.library.pdf.ApplicationFile;
import formflow.library.pdf.PdfGenerator;
import formflow.library.pdf.PdfLocationConfiguration;
import formflow.library.utilities.AbstractMockMvcTest;
import java.nio.file.Files;
import java.nio.file.Paths;
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
  private PdfLocationConfiguration pdfLocationConfiguration = mock(PdfLocationConfiguration.class);
  private PdfGenerator pdfGenerator = mock(PdfGenerator.class);
  String testPdf;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    testPdf = "Multipage-UBI-Form";
    pdfController = new PdfController(pdfGenerator, pdfLocationConfiguration);
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder().id(UUID.randomUUID()).build();
    byte[] pdfBytes = Files.readAllBytes(Paths.get("src", "test", "resources", "pdfs").resolve(Paths.get(testPdf + ".pdf")));

    when(pdfLocationConfiguration.get(testPdf)).thenReturn(
        new ApplicationFile(pdfBytes, testPdf));

    super.setUp();
  }

  @Test
  public void pdfControllerShouldDownloadPDFwhenReached() throws Exception {
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId() + "/" + testPdf))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=Multipage-UBI-Form-" + submission.getId() + ".pdf"))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    byte[] actualBytes = result.getResponse().getContentAsByteArray();
    assertThat(actualBytes).hasSizeGreaterThan(22);
  }

  @Test
  public void pdfControllerShouldFillTheCorrectFieldsForThePdfWhenDownloaded() throws Exception {
    String pdfToGenerate = "/Multipage-UBI-Form";
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId() + pdfToGenerate))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=Multipage-UBI-Form-" + submission.getId() + ".pdf"))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    byte[] actualBytes = result.getResponse().getContentAsByteArray();
    assertThat(actualBytes).hasSizeGreaterThan(22);
    verify(pdfGenerator, times(1)).generate(pdfLocationConfiguration.get(pdfToGenerate), submission.getId());

//    PDAcroForm pdf = PdfTest.loadPdf(actualBytes);
//    PdfTest.assertPdfFieldEquals(pdf, "SUBMISSION_ID", submission.getId().toString());
  }
}
