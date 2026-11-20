package formflow.library.controller_advisors;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Catches an {@link ObjectOptimisticLockingFailureException} thrown anywhere in a form-flow request (e.g. two
 * concurrent requests both trying to update the same {@link formflow.library.data.Submission}) and redirects the
 * user back to the home page with a flash message explaining that their submission was updated elsewhere.
 */
@ControllerAdvice
@Slf4j
public class SubmissionConflictAdvice {

    private MessageSource messageSource;

    /**
     * Creates the advice.
     *
     * @param messageSource source for the user-facing "submission conflict" message
     */
    public SubmissionConflictAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Redirects to the home page with a "submission conflict" flash message.
     *
     * @param ex                 the exception that triggered this handler
     * @param redirectAttributes used to set the flash attributes the home page reads to show the message
     * @param locale             the language to look up the "submission conflict" message in
     * @return a redirect to the home page
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ModelAndView handleSubmissionConflict(ObjectOptimisticLockingFailureException ex,
            RedirectAttributes redirectAttributes, Locale locale) {
        log.info("Submission was concurrently updated elsewhere: {}", ex.getMessage());

        redirectAttributes.addFlashAttribute("submissionConflict", true);
        redirectAttributes.addFlashAttribute("submissionConflictMessage",
                messageSource.getMessage("error.submission-conflict", null, locale));

        return new ModelAndView("redirect:/");
    }
}
