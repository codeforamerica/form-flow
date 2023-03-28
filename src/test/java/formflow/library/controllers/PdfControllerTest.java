package formflow.library.controllers;

import formflow.library.PdfController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class PdfControllerTest extends AbstractMockMvcTest {

  private Submission submission;
  private MockMvc mockMvc;
  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;

  @Autowired
  private PdfController pdfController;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
    submission = Submission.builder().id(1L).build();
    super.setUp();
  }

//  @Test
//  public void pdfControllerShouldReturnMessageWhenReached() {
//
//  }
}
