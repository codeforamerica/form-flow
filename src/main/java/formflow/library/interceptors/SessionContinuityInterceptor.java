package formflow.library.interceptors;

import formflow.library.FormFlowController;
import formflow.library.config.FlowConfiguration;
import formflow.library.exceptions.LandmarkNotSetException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
 * This interceptor prevents users from jumping to random pages in a flow.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "form-flow.session-continuity-interceptor.enabled", havingValue = "true")
public class SessionContinuityInterceptor implements HandlerInterceptor, Ordered {

  public static final String FLOW_PATH_FORMAT = "/flow/{flow}/{screen}";
  public static final String NAVIGATION_FLOW_PATH_FORMAT = "/flow/{flow}/{screen}/navigation";

  private static final String REDIRECT_URL = "/";

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
   * @throws IOException             - thrown in the event that an input or output exception occurs when this method does a
   *                                 redirect.
   * @throws LandmarkNotSetException - thrown in the event that a landmark(s) screen is misconfigured
   */
  @Override
  public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler)
      throws IOException, LandmarkNotSetException {
    String pathFormat = request.getRequestURI().contains("navigation") ? NAVIGATION_FLOW_PATH_FORMAT : FLOW_PATH_FORMAT;
    Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(pathFormat, request.getRequestURI());

    HttpSession session = request.getSession(false);

    FlowConfiguration flowConfiguration = flowConfigurations.stream()
        .filter(fc -> fc.getName().equals(parsedUrl.get("flow")))
        .findFirst()
        .orElse(null);

    if (flowConfiguration == null) {
      return true;
    }

    if (flowConfiguration.getLandmarks() == null) {
      throw new LandmarkNotSetException(
          "The SessionContinuityInterceptor is enabled, but no 'landmarks' section has been created in the application's form " +
              "flows configuration file.");
    }

    if (flowConfiguration.getLandmarks().getFirstScreen() == null) {
      throw new LandmarkNotSetException(
          "The SessionContinuityInterceptor is enabled, but a 'firstScreen' page has not " +
              "been identified in the 'landmarks' section in the application's form flows configuration file.");
    }

    String firstScreen = flowConfiguration.getLandmarks().getFirstScreen();

    if (!flowConfiguration.getFlow().containsKey(firstScreen)) {
      throw new LandmarkNotSetException(String.format(
          "The form flows configuration file does not contain a screen with the name '%s'. " +
              "Please make sure to correctly set the 'firstScreen' in the form flows configuration file 'landmarks' section.",
          firstScreen));
    }

    if (session == null) {
      if (parsedUrl.get("screen").equals(firstScreen)) {
        return true;
      }
      log.error("No active session found for request to {}. Redirecting to landing page.", request.getRequestURI());
      response.sendRedirect(REDIRECT_URL);
      return false;
    }

    if (FormFlowController.getSubmissionIdForFlow(session, parsedUrl.get("flow")) == null) {
      log.error("A submission ID was not found in the session for request to {}. Redirecting to landing page.",
          request.getRequestURI());
      response.sendRedirect(REDIRECT_URL);
      return false;
    }

    return true;
  }

  /**
   * Sets the value of the interceptor to the highest integer value making it the last interceptor executed.
   *
   * @return Max Integer value.
   */
  @Override
  public int getOrder() {
    return Integer.MAX_VALUE;
  }
}
