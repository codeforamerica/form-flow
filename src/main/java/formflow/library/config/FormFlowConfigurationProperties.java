package formflow.library.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "form-flow")
@Getter
@Setter
public class FormFlowConfigurationProperties {
  private List<Map<String, String>> disabledFlows = new ArrayList<>();
  private List<Map<String, String>  > lockAfterSubmitted = new ArrayList<>();


  /**
   * Checks if the flow with flowName is disabled.
   * @param flowName the name of the flow to check.
   * @return true if the flow is disabled, false otherwise.
   */
  public boolean isFlowDisabled(String flowName) {
    return this.disabledFlows.stream().anyMatch(flow -> flow.get("flow").equals(flowName));
  }
  
  /**
   * Gets the static redirect page for a disabled flow.
   * @param flowName the name of the flow to check.
   * @return the static redirect page for the flow if disabled flows are configured and the flow is disabled.
   */
  public String getDisabledFlowRedirect(String flowName) {
    return disabledFlows.stream()
        .filter(flow -> flow.get("flow").equals(flowName))
        .map(flow -> flow.getOrDefault("staticRedirectPage", ""))
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks if the Submission for the flow with flowName should be locked, preventing further updates after it has been submitted.
   * @param flowName the name of the flow to check.
   * @return true if the Submission should be locked for the given flow, false otherwise.
   */
  public boolean isSubmissionLockedForFlow(String flowName) {
    return this.lockAfterSubmitted.stream().anyMatch(flow -> flow.get("flow").equals(flowName));
  }

  /**
   * Gets the redirect screen designated for the given flow with a locked submission.
   * @param flowName the name of the flow to check.
   * @return the screen to redirect to if the submission is locked for the given flow.
   */
  public String getLockedSubmissionRedirect(String flowName) {
    return lockAfterSubmitted.stream()
        .filter(flow -> flow.get("flow").equals(flowName))
        .map(flow -> flow.getOrDefault("submissionLockedRedirectPage", ""))
        .findFirst()
        .orElse(null);
  }
}
