package formflow.library.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "form-flow.lock-after-submitted")
@Getter
@Setter
public class LockedSubmissionPropertyConfiguration {
  private List<Map<String, String>> lockAfterSubmitted = new ArrayList<>();


  /**
   * Checks if the flow with flowName is disabled.
   * @param flowName the name of the flow to check.
   * @return true if the flow is disabled, false otherwise.
   */
  public boolean isFlowSubmissionLocked(String flowName) {
    return this.lockAfterSubmitted.stream().anyMatch(flow -> flow.get("flow").equals(flowName));
  }

  /**
   * Gets the static redirect page for a disabled flow.
   * @param flowName the name of the flow to check.
   * @return the static redirect page for the flow if disabled flows are configured and the flow is disabled.
   */
  public String getRedirectForSubmissionLockedFlow(String flowName) {
    return lockAfterSubmitted.stream()
        .filter(flow -> flow.get("flow").equals(flowName))
        .map(flow -> flow.getOrDefault("submissionLockedRedirect", ""))
        .findFirst()
        .orElse(null);
  }
}
