package formflow.library.interceptors;

import formflow.library.ScreenController;
import formflow.library.config.DisabledFlowPropertyConfiguration;
import formflow.library.exceptions.LandmarkNotSetException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DisabledFlowInterceptor implements HandlerInterceptor, Ordered {
  public static final String PATH_FORMAT = ScreenController.FLOW + "/" + ScreenController.FLOW_SCREEN_PATH;

  DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration;
  
  public DisabledFlowInterceptor(DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration) {
    this.disabledFlowPropertyConfiguration = disabledFlowPropertyConfiguration;
  }

  /**
   * Redirects users to the configured screen if a flow is marked as disabled.
   * 
   * @param request current HTTP request
   * @param response current HTTP response
   * @param handler chosen handler to execute, for type and/or instance evaluation
   * @return
   * @throws LandmarkNotSetException
   */

  @Override
  public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
    List<Map<String, String>> disabledFlows = disabledFlowPropertyConfiguration .getDisabledFlows();
    System.out.println("Disabled flows is equal to: " + disabledFlows.toString());

    Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(PATH_FORMAT, request.getRequestURI());
    String requestedFlow = parsedUrl.get("flow");
    if (disabledFlowPropertyConfiguration.isFlowDisabled(requestedFlow)) {
      String staticRedirectScreen = disabledFlowPropertyConfiguration.getDisabledFlowRedirect(requestedFlow);
      log.info("Flow %s is disabled. Redirecting to %s.".formatted(requestedFlow, staticRedirectScreen));

      try {
        response.sendRedirect(staticRedirectScreen);
      } catch (IOException e1) {
        log.error("Error redirecting to %s, going home instead".formatted(staticRedirectScreen));
        try {
          response.sendRedirect("/");
        } catch (IOException e2) {
          throw new RuntimeException(e1);
        }
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
