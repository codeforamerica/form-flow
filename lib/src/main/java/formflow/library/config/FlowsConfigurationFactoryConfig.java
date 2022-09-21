package formflow.library.config;

import formflow.library.FormFlowProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(FormFlowProperties.class)
public class FlowsConfigurationFactoryConfig {
    @Autowired
    private FormFlowProperties properties;

    @Bean
    public FlowsConfigurationFactory flowsConfigurationFactory() {
        return new FlowsConfigurationFactory(properties);
    }

    @Bean
    public List<FlowConfiguration> flowsConfiguration() {
        return flowsConfigurationFactory().getObject();
    }
}
