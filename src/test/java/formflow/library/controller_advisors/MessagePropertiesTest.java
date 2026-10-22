package formflow.library.controller_advisors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises the real {@link MessageSource} bean (rather than a mock) for message keys referenced by
 * {@link org.springframework.web.bind.annotation.ControllerAdvice} classes, so a missing/misspelled key can't ship
 * unnoticed the way {@code error.submission-conflict} did - a mocked {@link MessageSource} in a unit test doesn't
 * catch that the key was never actually defined in the properties files.
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
class MessagePropertiesTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    void sessionExpiredMessageResolvesForEnglishAndSpanish() {
        assertMessageResolves("error.session-expired", Locale.US);
        assertMessageResolves("error.session-expired", new Locale("es"));
    }

    @Test
    void submissionConflictMessageResolvesForEnglishAndSpanish() {
        assertMessageResolves("error.submission-conflict", Locale.US);
        assertMessageResolves("error.submission-conflict", new Locale("es"));
    }

    /**
     * A missing key here doesn't throw - the configured {@link MessageSource} falls back to returning the code
     * itself, which is non-blank and would pass a naive {@code isNotBlank()} check. Assert it's not just an echo of
     * the code, so a missing translation actually fails this test.
     */
    private void assertMessageResolves(String code, Locale locale) {
        String resolved = messageSource.getMessage(code, null, locale);
        assertThat(resolved).isNotBlank();
        assertThat(resolved).as("message for '%s' in locale '%s' should not just echo the code back", code, locale)
                .isNotEqualTo(code);
    }
}
