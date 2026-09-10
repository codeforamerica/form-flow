package formflow.library.controller_advisors;

import formflow.library.exceptions.SessionExpiredException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Catches a {@link SessionExpiredException} thrown anywhere in a form-flow request and redirects the user back
 * to the home page with a flash message explaining that their session expired.
 */
@ControllerAdvice
@Slf4j
public class SessionExpiredAdvice {

    private MessageSource messageSource;

    /**
     * Creates the advice.
     *
     * @param messageSource source for the user-facing "session expired" message
     */
    public SessionExpiredAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Redirects to the home page with a "session expired" flash message.
     *
     * @param ex                 the exception that triggered this handler
     * @param redirectAttributes used to set the flash attributes the home page reads to show the message
     * @param locale             the language to look up the "session expired" message in
     * @return a redirect to the home page
     */
    @ExceptionHandler(SessionExpiredException.class)
    public ModelAndView handleSessionExpired(SessionExpiredException ex, RedirectAttributes redirectAttributes, Locale locale) {
        log.info("Session expired: {}", ex.getMessage());

        redirectAttributes.addFlashAttribute("sessionExpired", true);
        redirectAttributes.addFlashAttribute("sessionExpiredMessage", messageSource.getMessage("error.session-expired", null, locale));

        return new ModelAndView("redirect:/");
    }
}