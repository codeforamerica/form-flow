package formflow.library.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to remove BasicErrorController bean definition so that
 * FormFlowErrorController can handle all error requests without conflicts.
 */
@Configuration
public class ErrorControllerConfiguration implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // Remove BasicErrorController if it exists, so FormFlowErrorController can handle errors
        if (registry.containsBeanDefinition("basicErrorController")) {
            registry.removeBeanDefinition("basicErrorController");
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }
}

