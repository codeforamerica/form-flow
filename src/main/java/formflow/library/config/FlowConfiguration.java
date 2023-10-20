package formflow.library.config;

import java.util.HashMap;

import lombok.Data;
import lombok.Getter;

/**
 * Represents the configuration for a certain flow.
 */
@Data
@Getter
public class FlowConfiguration {

  private String name;

  private HashMap<String, ScreenNavigationConfiguration> flow;

  private HashMap<String, SubflowConfiguration> subflows;

  private LandmarkConfiguration landmarks;

  /**
   * Returns the screen navigation for a particular screen.
   *
   * @param screenName name of the screen to get the flow for, not null
   * @return the navigation configuration for the particular screen
   */
  public ScreenNavigationConfiguration getScreenNavigation(String screenName) {
    return flow.get(screenName);
  }
}
