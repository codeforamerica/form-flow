package formflow.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FlowsConfigurationFactoryConfig {

  @Bean
  public FlowsConfigurationFactory flowsConfigurationFactory() {
    return new FlowsConfigurationFactory();
  }

  @Bean
  public List<FlowConfiguration> flowsConfiguration() {
    return flowsConfigurationFactory().getObject();
  }
}
