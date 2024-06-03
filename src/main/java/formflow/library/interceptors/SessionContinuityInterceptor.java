package formflow.library.interceptors;

import formflow.library.FormFlowController;
import formflow.library.ScreenController;
import formflow.library.config.FlowConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * This interceptor prevents users with an invalid session from jumping to random pages in a flow.
 */
@Slf4j
@ConditionalOnProperty(name = "form-flow.session-continuity-interceptor.enabled", havingValue = "true")
public class SessionContinuityInterceptor implements HandlerInterceptor, Ordered {

  public static final String FLOW_PATH_FORMAT = ScreenController.FLOW + "/" + ScreenController.FLOW_SCREEN_PATH;
  public static final String NAVIGATION_FLOW_PATH_FORMAT = FLOW_PATH_FORMAT + "/navigation";
  private final String redirectUrl;
  public List<FlowConfiguration> flowConfigurations;

  public SessionContinuityInterceptor(List<FlowConfiguration> flowConfigurations, String redirectUrl) {
    this.flowConfigurations = flowConfigurations;
    this.redirectUrl = redirectUrl;
  }

  /**
   * @param request  current HTTP request
   * @param response current HTTP response
   * @param handler  chosen handler to execute, for type and/or instance evaluation
   * @return Boolean True - allows the request to proceed to the ScreenController, False - stops the request from reaching the
   * Screen Controller.
   * @throws IOException - thrown in the event that an input or output exception occurs when this method does a
   *                     redirect.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler)
      throws IOException {
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

    String firstScreen = flowConfiguration.getLandmarks().getFirstScreen();

    if (session == null) {
      if (parsedUrl.get("screen").equals(firstScreen)) {
        return true;
      }
      log.error("No active session found for request to {}. Redirecting to landing page.", request.getRequestURI());
      response.sendRedirect(redirectUrl);
      return false;
    }

    UUID submissionId = null;
    try {
      submissionId = FormFlowController.getSubmissionIdForFlow(session, parsedUrl.get("flow"));
    } catch (ResponseStatusException ignored) {
    }

    if (submissionId == null && !parsedUrl.get("screen").equals(firstScreen)) {
      log.error("A submission ID was not found in the session for request to {}. Redirecting to landing page.",
          request.getRequestURI());
      response.sendRedirect(redirectUrl);
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
