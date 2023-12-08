package formflow.library.interceptors;

import formflow.library.ScreenController;
import formflow.library.config.FormFlowConfigurationProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

  /**
   * A URL path, set to: {@code flow/<flow>/<screen>}.
   */
  public static final String PATH_FORMAT = ScreenController.FLOW + "/" + ScreenController.FLOW_SCREEN_PATH;

  /**
   * A class that has all form flow configuration properties.
   */
  FormFlowConfigurationProperties formFlowConfigurationProperties;


  /**
   * Default constructor for DisabledFlowInterceptor. Sets {@code formFlowConfigurationProperties}.
   *
   * @param formFlowConfigurationProperties The configuration properties for form flow.
   */
  public DisabledFlowInterceptor(FormFlowConfigurationProperties formFlowConfigurationProperties) {
    this.formFlowConfigurationProperties = formFlowConfigurationProperties;
  }

  /**
   * Redirects users to the configured screen if a flow is marked as disabled.
   *
   * @param request  current HTTP request
   * @param response current HTTP response
   * @param handler  chosen handler to execute, for type and/or instance evaluation
   * @return true if the flow is enabled, which will pass the request to the next registered interceptor. False if the flow is
   * disabled, which will redirect the user to the configured staticRedirectPage, and stop the interceptor chain.
   */

  @Override
  public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
    Map<String, String> parsedUrl = new AntPathMatcher().extractUriTemplateVariables(PATH_FORMAT, request.getRequestURI());
    String requestedFlow = parsedUrl.get("flow");
    if (formFlowConfigurationProperties.isFlowDisabled(requestedFlow)) {
      String staticRedirectPage = formFlowConfigurationProperties.getDisabledFlowRedirect(requestedFlow);
      if (staticRedirectPage == null || staticRedirectPage.isEmpty()) {
        log.warn("Flow %s is disabled but no static redirect page is configured. Going home instead.".formatted(requestedFlow));
        staticRedirectPage = "/disabledFeature";
      }
      log.info("Flow %s is disabled. Redirecting to %s.".formatted(requestedFlow, staticRedirectPage));

      try {
        response.sendRedirect(staticRedirectPage);
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
