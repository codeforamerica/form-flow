package formflow.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Factory for FlowsConfiguration configuration.
 */
@Configuration
public class FlowsConfigurationFactoryConfig {

  /**
   * Bean to get a FlowsConfigurationFactory object.
   *
   * @return flow configuration factory
   */
  @Bean
  public FlowsConfigurationFactory flowsConfigurationFactory() {
    return new FlowsConfigurationFactory();
  }

  /**
   * Bean to get a list of FlowConfiguration objects.
   *
   * @return list of flow configuration objects
   */
  @Bean
  public List<FlowConfiguration> flowsConfiguration() {
    return flowsConfigurationFactory().getObject();
  }
}
