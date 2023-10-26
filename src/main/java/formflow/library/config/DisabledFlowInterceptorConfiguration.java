package formflow.library.config;


import static formflow.library.interceptors.DisabledFlowInterceptor.PATH_FORMAT;

import formflow.library.interceptors.DisabledFlowInterceptor;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/***
 * Adds DisabledFlowInterceptorConfiguration to the Interceptor registry.
 */
@Configuration
//@ConditionalOnProperty(name = "form-flow.disabled-flows", havingValue = "*", matchIfMissing = false)
public class DisabledFlowInterceptorConfiguration implements WebMvcConfigurer {

  DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration;
  
  public DisabledFlowInterceptorConfiguration(DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration) {
    this.disabledFlowPropertyConfiguration = disabledFlowPropertyConfiguration;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new DisabledFlowInterceptor(disabledFlowPropertyConfiguration))
        .addPathPatterns(List.of(PATH_FORMAT));
  }
}
