package formflow.library.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ConditionalOnProperty(name = "form-flow.disabled-flows")
@ConfigurationProperties(prefix = "form-flow")
@Getter
@Setter
public class DisabledFlowPropertyConfiguration {
  private List<Map<String, String>> disabledFlows = new ArrayList<>();


  /**
   * Checks if the flow with flowName is disabled.
   * @param flowName the name of the flow to check.
   * @return true if the flow is disabled, false otherwise.
   */
  public boolean isFlowDisabled(String flowName) {
    return this.disabledFlows.stream().anyMatch(flow -> flow.get("flow").equals(flowName));
  }
  
  /**
   * Gets the static redirect screen for a disabled flow.
   * @param flowName the name of the flow to check.
   * @return the static redirect screen for the flow if disabled flows are configured and the flow is disabled.
   */
  public String getDisabledFlowRedirect(String flowName) {
    if (disabledFlows == null) {
      return null;
    }
    Optional<Map<String, String>> disabledFlow = disabledFlows.stream().filter(flow -> flow.get("flow").equals(flowName)).findFirst();
    return disabledFlow.isPresent() ? disabledFlow.get().get("staticRedirectScreen") : null;
  }
}
