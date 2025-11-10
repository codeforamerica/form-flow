package formflow.library.languages;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import formflow.library.utilities.AbstractMockMvcTest;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;


public class LanguagesTest extends AbstractMockMvcTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    void shouldGetEnglishTranslation() {
        String continueText = messageSource.getMessage("general.inputs.continue", null, Locale.ENGLISH);

        assertThat(continueText).isEqualTo("Continue");
    }

    @Test
    void shouldGetSpanishTranslation() {
        String continueText = messageSource.getMessage("general.inputs.continue", null, Locale.forLanguageTag("es"));

        assertThat(continueText).isEqualTo("Continuar");
    }
}
