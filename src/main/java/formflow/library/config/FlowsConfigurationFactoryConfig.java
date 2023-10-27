package formflow.library.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * Factory for FlowsConfiguration configuration.
 */
@Configuration
public class FlowsConfigurationFactoryConfig {

  @Autowired(required = false)
  DisabledFlowPropertyConfiguration disabledFlowPropertyConfiguration;


  /**
   * Bean to get a FlowsConfigurationFactory object.
   *
   * @return flow configuration factory
   */
  @Bean
  public FlowsConfigurationFactory flowsConfigurationFactory() {
    if (this.disabledFlowPropertyConfiguration == null) {
      return new FlowsConfigurationFactory();
    }
    return new FlowsConfigurationFactory(disabledFlowPropertyConfiguration);
  }

  /**
   * Bean to get a list of FlowConfiguration objects.
   *
   * @return list of flow configuration objects
   */
  @Bean
  public List<FlowConfiguration> flowsConfiguration() throws IOException {
    return flowsConfigurationFactory().getObject();
  }
}
