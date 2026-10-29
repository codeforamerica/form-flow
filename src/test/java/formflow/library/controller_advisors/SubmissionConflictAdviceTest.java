package formflow.library.controller_advisors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

class SubmissionConflictAdviceTest {

    @Test
    void redirectsHomeWithAFlashMessageWhenAnOptimisticLockConflictOccurs() {
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(eq("error.submission-conflict"), any(), eq(Locale.US)))
                .thenReturn("Someone else updated this in the meantime, please try again.");
        SubmissionConflictAdvice advice = new SubmissionConflictAdvice(messageSource);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException("submissions", "some-id");

        ModelAndView modelAndView = advice.handleSubmissionConflict(ex, redirectAttributes, Locale.US);

        assertThat(modelAndView.getViewName()).isEqualTo("redirect:/");
        verify(redirectAttributes).addFlashAttribute("submissionConflict", true);
        verify(redirectAttributes).addFlashAttribute("submissionConflictMessage",
                "Someone else updated this in the meantime, please try again.");
    }
}
