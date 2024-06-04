package formflow.library.listeners;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionListenerLogger implements HttpSessionListener {

    public SessionListenerLogger() {
        log.info("SessionListenerLogger initialized");
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("Session created: ID={}", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.info("Session destroyed: ID={}", se.getSession().getId());
    }
}
