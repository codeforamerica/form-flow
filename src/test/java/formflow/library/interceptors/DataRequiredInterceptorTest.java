package formflow.library.interceptors;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

import formflow.library.config.FlowConfiguration;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-landmark-flow.yaml"})
class DataRequiredInterceptorTest extends AbstractMockMvcTest {
  
  @Mock
  FlowConfiguration testFlowConfiguration;
  
  
  @Test
  void shouldReturnTheCorrectErrorIfNoLandmarkIsConfigured() throws Exception {
    when(testFlowConfiguration.getLandmarks()).thenReturn(null);
    mockMvc.perform(get("/flow/testLandmarkFlow/first"))
//        .andExpect(status().is5xxServerError())
        .andExpect(content().string(containsString("You have enabled session continuity interception but have not created a landmark section in your applications flow configuration file.")));
  }
}