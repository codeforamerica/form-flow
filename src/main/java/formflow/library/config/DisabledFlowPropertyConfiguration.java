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
//@ConditionalOnProperty(name = "form-flow.disabled-flows", havingValue = "*", matchIfMissing = false)
@ConfigurationProperties(prefix = "form-flow")
@Getter
@Setter
public class DisabledFlowPropertyConfiguration {
  private List<Map<String, String>> disabledFlows;

  public boolean isFlowDisabled(String flowName) {
    return this.disabledFlows.stream().anyMatch(flow -> flow.get("flow").equals(flowName));
  }
  
  public String getDisabledFlowRedirect(String flowName) {
    if (disabledFlows == null) {
      return null;
    }
    Optional<Map<String, String>> disabledFlow = disabledFlows.stream().filter(flow -> flow.get("flow").equals(flowName)).findFirst();
    return disabledFlow.isPresent() ? disabledFlow.get().get("staticRedirectScreen") : null;
  }
}
