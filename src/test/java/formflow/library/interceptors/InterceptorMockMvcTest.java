package formflow.library.interceptors;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.config.FlowConfiguration;
import formflow.library.config.LandmarkConfiguration;
import formflow.library.config.NextScreen;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.exceptions.LandmarkNotSetException;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-landmark-flow.yaml"})
@TestPropertySource(properties = {"form-flow.session-continuity-interceptor.enabled=true"})
@Import(SpyInterceptorConfig.class)
class InterceptorMockMvcTest extends AbstractMockMvcTest {

  @Autowired
  private LocaleChangeInterceptor localeChangeInterceptor;

  @Autowired
  private SessionContinuityInterceptor sessionContinuityInterceptor;

  @Override
  @BeforeEach
  protected void setUp() throws Exception {
    // These tests dirty the context of the data, so we need to reset it before each test.
    // We do this rather than use @DirtiesContext because it's much faster.
    sessionContinuityInterceptor.flowConfigurations = List.of();
    super.setUp();
  }

  @Test
  void shouldRunTheDataRequiredInterceptorLast() throws Exception {
    FlowConfiguration flowConfiguration = new FlowConfiguration();
    ScreenNavigationConfiguration screenNavigationConfiguration = new ScreenNavigationConfiguration();
    NextScreen nextScreen = new NextScreen();
    nextScreen.setName("first");
    screenNavigationConfiguration.setNextScreens(List.of(nextScreen));
    HashMap<String, ScreenNavigationConfiguration> screenNavigationConfigurations = new HashMap<>();
    screenNavigationConfigurations.put("first", screenNavigationConfiguration);
    flowConfiguration.setFlow(screenNavigationConfigurations);
    flowConfiguration.setName("testLandmarkFlow");
    LandmarkConfiguration landmarkConfiguration = new LandmarkConfiguration();
    landmarkConfiguration.setFirstScreen("first");
    flowConfiguration.setLandmarks(landmarkConfiguration);
    sessionContinuityInterceptor.flowConfigurations = List.of(flowConfiguration);
    mockMvc.perform(get("/flow/testLandmarkFlow/first?lang=es"))
        .andExpect(status().isOk());

    InOrder inOrder = inOrder(localeChangeInterceptor, sessionContinuityInterceptor);
    inOrder.verify(localeChangeInterceptor).preHandle(any(), any(), any());
    inOrder.verify(sessionContinuityInterceptor).preHandle(any(), any(), any());
  }

  @Test
  void shouldErrorIfLandmarkIsNotSet() throws Exception {
    FlowConfiguration flowConfiguration = new FlowConfiguration();
    flowConfiguration.setName("testLandmarkFlow");
    sessionContinuityInterceptor.flowConfigurations = List.of(flowConfiguration);
    mockMvc.perform(MockMvcRequestBuilders.get("/flow/testLandmarkFlow/first"))
        .andExpect(status().is5xxServerError())
        .andExpect(result -> {
          Exception resolvedException = result.getResolvedException();
          assertTrue(resolvedException instanceof LandmarkNotSetException, "Expected RuntimeException to be thrown");

          assertEquals(
              "The SessionContinuityInterceptor is enabled, but no 'landmarks' section has been created in the application's form flows configuration file.",
              resolvedException.getMessage());
        });
  }

  @Test
  void shouldErrorIfFirstScreenIsNotSet() throws Exception {
    FlowConfiguration flowConfiguration = new FlowConfiguration();
    flowConfiguration.setName("testLandmarkFlow");
    LandmarkConfiguration landmarkConfiguration = new LandmarkConfiguration();
    landmarkConfiguration.setFirstScreen(null);
    flowConfiguration.setLandmarks(landmarkConfiguration);
    sessionContinuityInterceptor.flowConfigurations = List.of(flowConfiguration);
    mockMvc.perform(MockMvcRequestBuilders.get("/flow/testLandmarkFlow/first"))
        .andExpect(status().is5xxServerError())
        .andExpect(result -> {
          Exception resolvedException = result.getResolvedException();
          assertTrue(resolvedException instanceof LandmarkNotSetException, "Expected RuntimeException to be thrown");
          assertEquals(
              "The SessionContinuityInterceptor is enabled, but a 'firstScreen' page has not been identified in the 'landmarks' section in the application's form flows configuration file.",
              resolvedException.getMessage());
        });
  }

  @Test
  void shouldErrorIfFirstScreenDoesNotExistWithinFlowConfiguration() throws Exception {
    FlowConfiguration flowConfiguration = new FlowConfiguration();
    ScreenNavigationConfiguration screenNavigationConfiguration = new ScreenNavigationConfiguration();
    NextScreen nextScreen = new NextScreen();
    nextScreen.setName("first");
    screenNavigationConfiguration.setNextScreens(List.of(nextScreen));
    HashMap<String, ScreenNavigationConfiguration> screenNavigationConfigurations = new HashMap<>();
    screenNavigationConfigurations.put("first", screenNavigationConfiguration);
    flowConfiguration.setFlow(screenNavigationConfigurations);
    flowConfiguration.setName("testLandmarkFlow");
    LandmarkConfiguration landmarkConfiguration = new LandmarkConfiguration();
    landmarkConfiguration.setFirstScreen("nonExistentScreen");
    flowConfiguration.setLandmarks(landmarkConfiguration);
    sessionContinuityInterceptor.flowConfigurations = List.of(flowConfiguration);

    mockMvc.perform(MockMvcRequestBuilders.get("/flow/testLandmarkFlow/first"))
        .andExpect(status().is5xxServerError())
        .andExpect(result -> {
          Exception resolvedException = result.getResolvedException();
          assertTrue(resolvedException instanceof LandmarkNotSetException, "Expected RuntimeException to be thrown");
          assertEquals(
              "The form flows configuration file does not contain a screen with the name 'nonExistentScreen'. Please make sure to correctly set the 'firstScreen' in the form flows configuration file 'landmarks' section.",
              resolvedException.getMessage());
        });
  }
}