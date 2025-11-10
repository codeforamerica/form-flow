package formflow.library.interceptors;

import formflow.library.config.FlowConfiguration;
import formflow.library.config.FormFlowConfigurationProperties;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@TestConfiguration
public class SpyInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private List<FlowConfiguration> flowConfigurations;

    @Autowired
    private FormFlowConfigurationProperties formFlowConfigurationProperties;

    @Bean
    @Primary
    public LocaleChangeInterceptor localeChangeInterceptor() {
        return Mockito.spy(new LocaleChangeInterceptor());
    }

    @Bean
    @Primary // Ensure this bean takes precedence over the real one
    public SessionContinuityInterceptor dataRequiredInterceptor() {
        return Mockito.spy(new SessionContinuityInterceptor(flowConfigurations));
    }

    @Bean
    @Primary
    public DisabledFlowInterceptor disabledFlowInterceptor() {
        return Mockito.spy(new DisabledFlowInterceptor(formFlowConfigurationProperties));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(dataRequiredInterceptor());
        registry.addInterceptor(disabledFlowInterceptor());
    }
}
