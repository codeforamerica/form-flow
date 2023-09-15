package formflow.library.interceptors;

import formflow.library.config.FlowConfiguration;
import formflow.library.exceptions.LandmarkNotSetException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
 * This interceptor prevents users from jumping to random pages in a flow.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "form-flow.session-continuity-interceptor.enabled", havingValue = "true")
public class SessionContinuityInterceptor implements HandlerInterceptor, Ordered {

  public static final String FLOW_PATH_FORMAT = "/flow/{flow}/{screen}";
  public static final String NAVIGATION_FLOW_PATH_FORMAT = "/flow/{flow}/{screen}/navigation";

  List<FlowConfiguration> flowConfigurations;

  public SessionContinuityInterceptor(List<FlowConfiguration> flowConfigurations) {
    this.flowConfigurations = flowConfigurations;
  }

  /**
   * @param request  current HTTP request
   * @param response current HTTP response
   * @param handler  chosen handler to execute, for type and/or instance evaluation
   * @return Boolean True - allows the request to proceed to the ScreenController, False - stops the request from reaching the
   * Screen Controller.
   * @throws Exception
   */
  @Override
  public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler)
      throws Exception {
    try {
      String pathFormat = request.getRequestURI().contains("navigation") ? NAVIGATION_FLOW_PATH_FORMAT : FLOW_PATH_FORMAT;
      Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(pathFormat, request.getRequestURI());
      String redirect_url = "/";
      String screen = parsedUrl.get("screen");
      String flow = parsedUrl.get("flow");
      HttpSession session = request.getSession(false);
      FlowConfiguration flowConfiguration = flowConfigurations.stream().filter(fc -> fc.getName().equals(flow)).findFirst()
          .orElse(null);

      if (flowConfiguration == null) {
        return true;
      }

      boolean landmarkNotImplemented = flowConfiguration.getLandmarks() == null;

      if (landmarkNotImplemented) {
        throw new LandmarkNotSetException(
            "You have enabled session continuity interception but have not created a landmark section in your applications flow configuration file.");
      }

      boolean firstScreenNotSet = flowConfiguration.getLandmarks().getFirstScreen() == null;

      if (firstScreenNotSet) {
        throw new LandmarkNotSetException(
            "Please make sure to set a firstScreen under your flow configuration files landmark section.");
      }

      String firstScreen = flowConfiguration.getLandmarks().getFirstScreen();
      boolean firstScreenExists = flowConfiguration.getFlow().containsKey(firstScreen);

      if (!firstScreenExists) {
        throw new LandmarkNotSetException(String.format(
            "Please make sure that you have correctly set the firstScreen under your flow configuration files landmark section. Your flow configuration file does not contain a screen with the name %s.",
            firstScreen));
      }

      boolean onFirstScreen = screen.equals(firstScreen);
      if (session == null && onFirstScreen) {
        return true;
      }

      if (session == null) {
        log.error("No active session found for request to {}. Redirecting to landing page.", request.getRequestURI());
        response.sendRedirect(redirect_url);
        return false;
      }

      if (session.getAttribute("id") == null) {
        log.error("A submission ID was not found in the session for request to {}. Redirecting to landing page.",
            request.getRequestURI());
        response.sendRedirect(redirect_url);
        return false;
      }

      return true;
    } catch (IllegalStateException e) {
      return true;
    }
  }

  /**
   * Sets the value of the interceptor to the highest integer value making it the last interceptor executed.
   *
   * @return Max Integer value.
   */
  @Override
  public int getOrder() {
    // Max value ensures that this interceptor is executed last.
    return Integer.MAX_VALUE;
  }
}
