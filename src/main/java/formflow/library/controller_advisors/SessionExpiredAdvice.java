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

@ControllerAdvice
@Slf4j
public class SessionExpiredAdvice {
    
    private MessageSource messageSource;

    public SessionExpiredAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ModelAndView handleSessionExpired(SessionExpiredException ex, RedirectAttributes redirectAttributes, Locale locale) {
        log.info("Session expired: {}", ex.getMessage());

        redirectAttributes.addFlashAttribute("sessionExpired", true);
        redirectAttributes.addFlashAttribute("sessionExpiredMessage", messageSource.getMessage("error.session-expired", null, locale));

        return new ModelAndView("redirect:/");
    }
}