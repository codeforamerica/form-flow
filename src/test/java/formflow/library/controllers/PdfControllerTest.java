package formflow.library.controllers;

import formflow.library.PdfController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.pdf.PdfFile;
import formflow.library.pdf.PdfGenerator;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PdfControllerTest extends AbstractMockMvcTest {

    private Submission submission;
    private MockMvc mockMvc;
    private final PdfGenerator pdfGenerator = mock(PdfGenerator.class);

    private SubmissionRepositoryService submissionRepositoryService;
    private PdfFile filledPdf;
    private String flow;


    @Override
    @BeforeEach
    public void setUp() throws Exception {
        flow = "ubi";
        PdfController pdfController = new PdfController(messageSource, pdfGenerator, submissionRepositoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(pdfController).build();
        submission = Submission.builder().id(UUID.randomUUID()).build();
        filledPdf = mock(PdfFile.class);
        when(filledPdf.fileBytes()).thenReturn(new byte[5]);
        when(pdfGenerator.generate(flow, submission)).thenReturn(filledPdf);
        super.setUp();
    }

    @Test
    public void getDownloadGeneratesAndReturnsFilledFlattenedPdf() throws Exception {
        session.setAttribute("id", submission.getId());
        MvcResult result = mockMvc.perform(get("/download/ubi/" + submission.getId()).session(session))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=%s-%s.pdf".formatted(filledPdf.name(), submission.getId())))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(filledPdf.fileBytes());

        verify(pdfGenerator, times(1)).generate(flow, submission);
        verify(filledPdf, times(1)).finalizeForSending();
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
