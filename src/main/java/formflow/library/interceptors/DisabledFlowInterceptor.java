package formflow.library.interceptors;

import formflow.library.ScreenController;
import formflow.library.config.DisabledFlowPropertyConfiguration;
import formflow.library.exceptions.LandmarkNotSetException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * This interceptor redirects users to the configured screen if a flow is marked as disabled.
 */
@Component
//@ConditionalOnProperty(name = "form-flow.disabled-flows", havingValue = "*", matchIfMissing = false)
public class DisabledFlowInterceptor implements HandlerInterceptor, Ordered {
  public static final String PATH_FORMAT = ScreenController.FLOW + "/" + ScreenController.FLOW_SCREEN_PATH;

  DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration;
  
  public DisabledFlowInterceptor(DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration) {
    this.disabledFlowPropertyConfiguration = disabledFlowPropertyConfiguration;
  }

  @Override
  public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws LandmarkNotSetException {
    List<Map<String, String>> disabledFlows = disabledFlowPropertyConfiguration .getDisabledFlows();
    System.out.println("Disabled flows is equal to: " + disabledFlows.toString());

    Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(PATH_FORMAT, request.getRequestURI());
    String requestedFlow = parsedUrl.get("flow");
    if (disabledFlowPropertyConfiguration.isFlowDisabled(requestedFlow)) {
      String staticRedirectScreen = disabledFlowPropertyConfiguration.getDisabledFlowRedirect(requestedFlow);
      System.out.printf("Redirecting to %s - flow %s is disabled%n", staticRedirectScreen, requestedFlow);

      try {
        response.sendRedirect(staticRedirectScreen);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return false;
    }
    return true;
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }
}
