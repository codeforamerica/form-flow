package formflow.library.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Factory for FlowsConfiguration configuration.
 */
@Configuration
public class FlowsConfigurationFactoryConfig {

  @Autowired(required = false)
  FormFlowConfigurationProperties formFlowConfigurationProperties;


  /**
   * Bean to get a FlowsConfigurationFactory object.
   *
   * @return flow configuration factory
   */
  @Bean
  public FlowsConfigurationFactory flowsConfigurationFactory() {
    if (this.formFlowConfigurationProperties == null) {
      return new FlowsConfigurationFactory();
    }
    return new FlowsConfigurationFactory(formFlowConfigurationProperties);
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
