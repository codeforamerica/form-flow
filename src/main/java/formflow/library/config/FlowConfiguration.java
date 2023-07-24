package formflow.library.config;

import java.util.HashMap;
import java.util.NoSuchElementException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the configuration for a certain flow.
 */
@Data
@Slf4j
public class FlowConfiguration {

  private String name;
  private HashMap<String, ScreenNavigationConfiguration> flow;
  private HashMap<String, SubflowConfiguration> subflows;

  /**
   * Returns the screen navigation for a particular screen.
   *
   * @param screenName name of the screen to get the flow for, not null
   * @return the navigation configuration for the particular screen
   */
  public ScreenNavigationConfiguration getScreen(String screenName) {
    log.info("getScreen: flow: " + flow + ", screen: " + screenName);
    ScreenNavigationConfiguration screenNavigationConfiguration = flow.get(screenName);

    if (screenNavigationConfiguration == null) {
      throw new NoSuchElementException("Could not find screen: " + screenName + " in flow: " + flow);
    }

    return screenNavigationConfiguration;
  }
}
