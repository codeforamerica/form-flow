package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import formflow.library.ScreenController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-cross-validation-action.yaml"})
public class CrossValidationTest extends AbstractMockMvcTest {

    Submission submission;

    private MockMvc mockMvc;

    @MockBean
    private SubmissionRepositoryService submissionRepositoryService;

    @Autowired
    private ScreenController screenController;

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(screenController).build();
        submission = Submission.builder().id(1L).inputData(new HashMap<>()).build();

        super.setUp();
        when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
        when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    }

    @Test
    void shouldAcceptEmailWithPreference() throws Exception {
        postExpectingSuccess("contactInfoPreference",
                Map.of(
                        "emailAddress", List.of("foo@bar.com"),
                        "contactMethod", List.of("emailPreferred")));
    }

//    @Test
//    void shouldSaveTotalIncome() throws Exception {
//        String subflowUuid = UUID.randomUUID().toString();
//
//        List<Map<String, Object>> subflowList = new ArrayList<>();
//
//        subflowList.add(Map.of("uuid", subflowUuid));
//        subflowList.add(Map.of("uuid", UUID.randomUUID().toString(),
//                "textInput", "2200",
//                "iterationIsComplete", true));
//        subflowList.add(Map.of("uuid", UUID.randomUUID().toString(),
//                "textInput", "3330",
//                "iterationIsComplete", true));
//
//        submission.getInputData().put("income", subflowList);
//
//        postToUrlExpectingSuccess("/testFlow/next", "/testFlow/subflowReview",
//                Map.of("textInput", List.of("1000")), subflowUuid);
//
//        assertThat(submission.getInputData().get("totalIncome")).isEqualTo(6530.0);
//    }
}
