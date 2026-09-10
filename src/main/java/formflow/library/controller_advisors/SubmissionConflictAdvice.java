package formflow.library.controller_advisors;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class SubmissionConflictAdvice {

    private MessageSource messageSource;

    public SubmissionConflictAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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
