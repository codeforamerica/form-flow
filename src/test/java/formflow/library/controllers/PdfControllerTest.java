package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.PdfController;
import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
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

  @Autowired
  private PdfController pdfController;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder().id(new UUID(1L, 2L)).build();

    super.setUp();
  }

  @Test
  public void pdfControllerShouldDownloadPDFwhenReached() throws Exception {
    MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId() + "/Multipage-UBI-Form"))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=Multipage-UBI-Form-" + submission.getId() + ".pdf"))
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    byte[] actualBytes = result.getResponse().getContentAsByteArray();
    assertThat(actualBytes).hasSizeGreaterThan(22);
  }
}
