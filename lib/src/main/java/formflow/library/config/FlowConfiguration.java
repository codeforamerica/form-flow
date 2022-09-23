package formflow.library.config;

import java.util.HashMap;
import lombok.Data;

@Data
public class FlowConfiguration {

  private String name;
  private HashMap<String, ScreenNavigationConfiguration> flow;

  private HashMap<String, SubflowConfiguration> subflows;

  public ScreenNavigationConfiguration getScreenNavigation(String screenName) {
    return flow.get(screenName);
  }
}
