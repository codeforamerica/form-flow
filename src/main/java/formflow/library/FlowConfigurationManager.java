package formflow.library;

import formflow.library.config.FlowConfiguration;
import formflow.library.config.SubflowConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class FlowConfigurationManager {

  private final List<FlowConfiguration> flowConfigurations;

  public FlowConfigurationManager(List<FlowConfiguration> flowConfigurations) {
    this.flowConfigurations = flowConfigurations;
  }

  public FlowConfiguration get(String flow) {
    try {
      FlowConfiguration flowConfig = flowConfigurations.stream().filter(
          flowConfiguration -> flowConfiguration.getName().equals(flow)
      ).toList().get(0);
      if (flowConfig == null) {
        throw new NoSuchElementException("Could not find flow=" + flow + " in templates");
      }
      return flowConfig;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new NoSuchElementException("Could not find flow=" + flow + " in templates");
    }
  }

  public Boolean isDeleteConfirmationScreen(String flow, String screen) {
    HashMap<String, SubflowConfiguration> subflows = get(flow).getSubflows();
    if (subflows == null) {
      return false;
    }
    return subflows.entrySet().stream()
        .anyMatch(subflow -> subflow.getValue().getDeleteConfirmationScreen().equals(screen));
  }

  public HashMap<String, SubflowConfiguration> getSubflows(String flow) {
    return get(flow).getSubflows();
  }
}
