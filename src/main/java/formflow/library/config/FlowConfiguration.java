package formflow.library.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;

/**
 * Represents the configuration for a certain flow.
 */
@Data
@Getter
public class FlowConfiguration {

  private String name;

  private Map<String, ScreenNavigationConfiguration> flow;

  private Map<String, SubflowConfiguration> subflows;

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

  public void setFlow(Map<String, ScreenNavigationConfiguration> screenMap) {
    flow = screenMap.entrySet().stream()
        .map(entry -> {
          entry.getValue().setName(entry.getKey());
          return entry;
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
