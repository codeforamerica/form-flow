package formflow.library.config;

import java.util.HashMap;

import lombok.Data;

/**
 * Represents the configuration for a certain flow.
 */
@Data
public class FlowConfiguration {

  private String name;

  private HashMap<String, ScreenNavigationConfiguration> flow;

  private HashMap<String, SubflowConfiguration> subflows;

  private String conditionsPath;

  private String actionsPath;

  private TemplateManager templateManager;

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
