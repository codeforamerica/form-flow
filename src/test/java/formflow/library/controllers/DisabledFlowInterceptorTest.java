package formflow.library.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.data.Submission;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
@TestPropertySource(properties = {
        "form-flow.disabled-flows[0].flow=testFlow",
        "form-flow.disabled-flows[0].staticRedirectPage=/",
        "form-flow.disabled-flows[1].flow=otherTestFlow",
        "form-flow.disabled-flows[1].staticRedirectPage=/disabledFeature",
})
public class DisabledFlowInterceptorTest extends AbstractMockMvcTest {

    @BeforeEach
    public void setUp() throws Exception {
        UUID submissionUUID = UUID.randomUUID();
        submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
        // this setups flow info in the session to get passed along later on.
        setFlowInfoInSession(session, "testFlow", submission.getId());
        super.setUp();
    }

    @Test
    public void shouldRedirectToConfiguredScreenWhenDisabledFlowInterceptionIsEnabled() throws Exception {
        mockMvc.perform(get("/flow/testFlow/inputs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertEquals("/", Objects.requireNonNull(result.getResponse().getRedirectedUrl())));
        mockMvc.perform(get("/flow/otherTestFlow/inputs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertEquals("/disabledFeature",
                        Objects.requireNonNull(result.getResponse().getRedirectedUrl())));
    }
}
