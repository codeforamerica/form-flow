package formflow.library.config;


import static formflow.library.interceptors.DisabledFlowInterceptor.PATH_FORMAT;

import formflow.library.interceptors.DisabledFlowInterceptor;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/***
 * Adds DisabledFlowInterceptorConfiguration to the Interceptor registry.
 */
@Configuration
public class DisabledFlowInterceptorConfiguration implements WebMvcConfigurer {

  FormFlowConfigurationProperties formFlowConfigurationProperties;

  /**
   * Default constructor for DisabledFlowInterceptorConfiguration. Sets {@code formFlowConfigurationProperties}.
   *
   * @param formFlowConfigurationProperties The configuration properties for form flow.
   */
  public DisabledFlowInterceptorConfiguration(FormFlowConfigurationProperties formFlowConfigurationProperties) {
    this.formFlowConfigurationProperties = formFlowConfigurationProperties;
  }

  /**
   * Adds the DisabledFlowInterceptor to the Interceptor registry.
   *
   * @param registry The Interceptor registry.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new DisabledFlowInterceptor(formFlowConfigurationProperties))
        .addPathPatterns(List.of(PATH_FORMAT));
  }
}
