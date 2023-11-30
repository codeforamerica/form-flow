package formflow.library.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import formflow.library.exceptions.FlowConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"form-flow.session-continuity-interceptor.enabled=true"})
public class FlowsConfigurationFactoryTest {

  private FlowsConfigurationFactory flowsConfigurationFactory;


  String AFTER_SUBMIT_MISCONFIGURATION_ERROR = "You have enabled submission locking for the flow testFlow but the afterSubmitPages landmark is not set in your flow configuration yaml file.";
  String FIRST_SCREEN_MISCONFIGURATION_ERROR = "You have enabled the session continuity interceptor in your application but have not added a first screen landmark for the flow testFlow in your flow configuration yaml file.";
  String INCORRECT_FIRST_SCREEN_NAME_ERROR = "Your flow configuration file for the flow testFlow does not contain a screen with the name 'doesNotExistInTheFlow'. You may have misspelled the screen name. Please make sure to correctly set the 'firstScreen' in the flows configuration file 'landmarks' section.";
  @BeforeEach
  public void setUp() {
    FormFlowConfigurationProperties formFlowConfigurationProperties = mock(FormFlowConfigurationProperties.class);
    flowsConfigurationFactory = new FlowsConfigurationFactory(formFlowConfigurationProperties);
  }

  @Test
  public void testValidateLandmarksAfterSubmitPages_ThrowsExceptionWhenLandmarksMissing() {
    // Create a flow that does not include a landmarks section at all
    FlowConfiguration flowConfig = new FlowConfiguration();
    flowConfig.setName("testFlow");
    // The factory should throw an exception when it tries to validate the flow since there is no landmarks section but submission locking is enabled
    FlowConfigurationException errorThrownForNoLandmarkSectionAtAll = assertThrows(FlowConfigurationException.class, () -> flowsConfigurationFactory.validateLandmarksAfterSubmitPages(flowConfig));
    assertEquals(errorThrownForNoLandmarkSectionAtAll.getMessage(), AFTER_SUBMIT_MISCONFIGURATION_ERROR);
    
    // Add a landmarks section that has no afterSubmitPages section, the factory should throw an exception due to the missing afterSubmitPages section
    LandmarkConfiguration landmarks = new LandmarkConfiguration();
    flowConfig.setLandmarks(landmarks);
    FlowConfigurationException errorThrownForNoAfterSubmitLandmark = assertThrows(FlowConfigurationException.class, () -> flowsConfigurationFactory.validateLandmarksAfterSubmitPages(flowConfig));
    assertEquals(errorThrownForNoAfterSubmitLandmark.getMessage(), AFTER_SUBMIT_MISCONFIGURATION_ERROR);
    
    // Add a landmarks section that has a null afterSubmitPages section, the factory should throw an exception due to the null afterSubmitPages section
    landmarks.setAfterSubmitPages(null);
    flowConfig.setLandmarks(landmarks);
    FlowConfigurationException errorThrownForEmptyAfterSubmitLandmark = assertThrows(FlowConfigurationException.class, () -> flowsConfigurationFactory.validateLandmarksAfterSubmitPages(flowConfig));
    assertEquals(errorThrownForEmptyAfterSubmitLandmark.getMessage(), AFTER_SUBMIT_MISCONFIGURATION_ERROR);
    
    // Add a landmarks section that has an afterSubmitPages section with at least one value, no error should be thrown
    landmarks.setAfterSubmitPages(new ArrayList<>(List.of("testAfterSubmitPage")));
    flowConfig.setLandmarks(landmarks);
    assertDoesNotThrow(() -> flowsConfigurationFactory.validateLandmarksAfterSubmitPages(flowConfig));
  }

  @Test
  void testValidateLandmarksFirstScreen_ThrowsExceptionWhenLandmarksMissing() {
    // Create a flow that does not include a landmarks section at all
    FlowConfiguration flowConfig = new FlowConfiguration();
    HashMap<String, ScreenNavigationConfiguration> screenNavigationConfigurations = new HashMap<>();
    screenNavigationConfigurations.put("testFirstScreen", new ScreenNavigationConfiguration());
    flowConfig.setFlow(screenNavigationConfigurations);
    flowConfig.setName("testFlow");

    // The factory should throw an exception when it tries to validate the flow since there is no landmarks section but session continuity interception is enabled
    FlowConfigurationException errorThrownForNoLandmarkSectionAtAll = assertThrows(FlowConfigurationException.class,
        () -> flowsConfigurationFactory.validateLandmarksFirstScreen(flowConfig));
    assertEquals(errorThrownForNoLandmarkSectionAtAll.getMessage(), FIRST_SCREEN_MISCONFIGURATION_ERROR);

    // Add a landmarks section that has no firstScreen section, the factory should throw an exception due to the missing firstScreen section
    LandmarkConfiguration landmarks = new LandmarkConfiguration();
    flowConfig.setLandmarks(landmarks);
    FlowConfigurationException errorThrownForNoFirstScreenLandmark = assertThrows(FlowConfigurationException.class,
        () -> flowsConfigurationFactory.validateLandmarksFirstScreen(flowConfig));
    assertEquals(errorThrownForNoFirstScreenLandmark.getMessage(), FIRST_SCREEN_MISCONFIGURATION_ERROR);

    // Add a landmarks section that has a null firstScreen section, the factory should throw an exception due to the null firstScreen section
    landmarks.setFirstScreen(null);
    flowConfig.setLandmarks(landmarks);
    FlowConfigurationException errorThrownForEmptyFirstScreenLandmark = assertThrows(FlowConfigurationException.class,
        () -> flowsConfigurationFactory.validateLandmarksFirstScreen(flowConfig));
    assertEquals(errorThrownForEmptyFirstScreenLandmark.getMessage(), FIRST_SCREEN_MISCONFIGURATION_ERROR);

    // Add a landmarks section that has an incorrect firstScreen name, the factory should throw an exception since the firstScreen name does not match any screen in the flow
    landmarks.setFirstScreen("doesNotExistInTheFlow");
    flowConfig.setLandmarks(landmarks);
    FlowConfigurationException errorThrownForIncorrectFirstScreenName = assertThrows(FlowConfigurationException.class,
        () -> flowsConfigurationFactory.validateLandmarksFirstScreen(flowConfig));
    assertEquals(errorThrownForIncorrectFirstScreenName.getMessage(), INCORRECT_FIRST_SCREEN_NAME_ERROR);
    
    // Add a landmarks section that has an firstScreen section with at least one value, no error should be thrown
    landmarks.setFirstScreen("testFirstScreen");
    flowConfig.setLandmarks(landmarks);
    assertDoesNotThrow(() -> flowsConfigurationFactory.validateLandmarksFirstScreen(flowConfig));
  }
}