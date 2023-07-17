package formflow.library.languages;

import formflow.library.utilities.AbstractMockMvcTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.junit.jupiter.api.Test;


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
