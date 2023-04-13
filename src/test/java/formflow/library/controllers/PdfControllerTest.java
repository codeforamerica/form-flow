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
import formflow.library.pdf.PdfMap;
import formflow.library.pdf.PdfMapConfiguration;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class PdfControllerTest extends AbstractMockMvcTest {

  private Submission submission;
  private MockMvc mockMvc;
  private PdfController pdfController;
  @Autowired
  private List<PdfMapConfiguration> pdfMapConfigurations;
  private PdfGenerator pdfGenerator = mock(PdfGenerator.class);
  private String testPdf;
  private ApplicationFile emptyPdf;
  private ApplicationFile filledPdf;
  private PdfMap pdfMap;


  @Override
  @BeforeEach
  public void setUp() throws Exception {
    testPdf = "Multipage-UBI-Form";
    pdfMap = new PdfMap(pdfMapConfigurations);
    pdfController = new PdfController(pdfGenerator, pdfMap);
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder().id(UUID.randomUUID()).build();

    emptyPdf = new ApplicationFile(new byte[1], testPdf);
    filledPdf = new ApplicationFile(new byte[2], testPdf + "-filled");

    when(pdfGenerator.generate(emptyPdf, submission.getId())).thenReturn(filledPdf);

    super.setUp();
  }

  @Test
  public void getDownloadGeneratesAndReturnsFilledPdf() throws Exception {
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId()))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=%s-%s.pdf".formatted(filledPdf.fileName(), submission.getId())))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdf.fileBytes());
    verify(pdfGenerator, times(1)).generate(emptyPdf, submission.getId());
  }
}
