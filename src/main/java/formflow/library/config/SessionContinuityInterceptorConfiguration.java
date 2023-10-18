package formflow.library.config;

import formflow.library.interceptors.SessionContinuityInterceptor;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/***
 * Adds SessionContinuityInterceptor to the Interceptor registry.
 */
@Configuration
@ConditionalOnProperty(name = "form-flow.session-continuity-interceptor.enabled", havingValue = "true")
public class SessionContinuityInterceptorConfiguration implements WebMvcConfigurer {

  @Autowired
  List<FlowConfiguration> flowConfigurations;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new SessionContinuityInterceptor(flowConfigurations))
        .addPathPatterns(List.of(SessionContinuityInterceptor.FLOW_PATH_FORMAT,
            SessionContinuityInterceptor.NAVIGATION_FLOW_PATH_FORMAT));
  }
}
