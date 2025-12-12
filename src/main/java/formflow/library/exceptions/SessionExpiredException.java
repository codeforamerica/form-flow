package formflow.library.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SessionExpiredException extends ResponseStatusException {

    public SessionExpiredException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
