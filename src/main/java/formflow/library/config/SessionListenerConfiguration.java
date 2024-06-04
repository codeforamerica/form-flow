package formflow.library.config;

import formflow.library.listeners.SessionListenerLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SessionListenerConfiguration {
    
    @Bean(name = "sessionListenerLogger")
    public ServletListenerRegistrationBean<SessionListenerLogger> sessionListener() {
        log.info("Registering SessionListenerLogger");
        return new ServletListenerRegistrationBean<>(new SessionListenerLogger());
    }
}
