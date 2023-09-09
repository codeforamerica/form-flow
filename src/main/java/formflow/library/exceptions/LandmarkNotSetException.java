package formflow.library.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class LandmarkNotSetException extends RuntimeException {
    public LandmarkNotSetException(String message) {
        super(message);
    }
}
