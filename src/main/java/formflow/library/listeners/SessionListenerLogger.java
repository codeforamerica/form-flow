package formflow.library.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionListenerLogger {

    @EventListener
    public void onSessionCreated(HttpSessionCreatedEvent event) {
        log.info("Session created: ID={}", event.getSession().getId());
    }

    @EventListener
    public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
        log.info("Session destroyed: ID={}", event.getId());
    }
}
