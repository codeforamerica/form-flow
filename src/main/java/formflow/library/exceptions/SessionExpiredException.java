package formflow.library.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a form-flow request needs a {@link jakarta.servlet.http.HttpSession} (or a submission referenced
 * by it) that's missing, most commonly because the user's session expired. Caught by
 * {@link formflow.library.controller_advisors.SessionExpiredAdvice} and translated into a redirect to the home
 * page.
 */
public class SessionExpiredException extends ResponseStatusException {

    /**
     * Creates the exception.
     *
     * @param message a message describing what was missing, for logging - not shown to the user
     */
    public SessionExpiredException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
