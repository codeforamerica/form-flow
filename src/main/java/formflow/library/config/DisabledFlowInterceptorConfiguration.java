package formflow.library.config;


import static formflow.library.interceptors.DisabledFlowInterceptor.PATH_FORMAT;

import formflow.library.interceptors.DisabledFlowInterceptor;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/***
 * Adds DisabledFlowInterceptorConfiguration to the Interceptor registry.
 */
@Configuration
public class DisabledFlowInterceptorConfiguration implements WebMvcConfigurer {
  
  DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration;
  
  public DisabledFlowInterceptorConfiguration(DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration) {
    this.disabledFlowPropertyConfiguration = disabledFlowPropertyConfiguration;
  }
  
  /**
   * Adds the DisabledFlowInterceptor to the Interceptor registry.
   * @param registry The Interceptor registry.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new DisabledFlowInterceptor(disabledFlowPropertyConfiguration))
        .addPathPatterns(List.of(PATH_FORMAT));
  }
}