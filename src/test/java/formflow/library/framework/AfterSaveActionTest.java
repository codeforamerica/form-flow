package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.ScreenController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepository;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-after-save-action.yaml"})
public class AfterSaveActionTest extends AbstractMockMvcTest {

    Submission submission;

    @MockitoBean
    private SubmissionRepositoryService submissionRepositoryService;

    @MockitoBean
    private SubmissionRepository submissionRepository;

    @Autowired
    private ScreenController screenController;

    @BeforeEach
    public void setUp() throws Exception {
        UUID submissionUUID = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(screenController).build();
        submission = Submission.builder().id(submissionUUID).inputData(new HashMap<>()).build();

        super.setUp();
        when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    }

    @Test
    void callsAfterSaveActionAfterSave() throws Exception {
        postWithoutData("testFlow", "inputs").andExpect(redirectedUrl(getUrlForPageName("testFlow", "inputs") + "/navigation"));

        verify(submissionRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    void callsAfterSaveActionAfterSaveInSubflow() throws Exception {

        ResultActions test = postWithoutData("testFlow", "subflowIterationStart/new");
        String url = test.andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        assertThat(url).isNotNull();
        assertThat(url.contains("next"));

        verify(submissionRepository, times(1)).findById(any(UUID.class));
    }
}
