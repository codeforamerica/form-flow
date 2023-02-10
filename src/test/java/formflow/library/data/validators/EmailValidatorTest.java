package formflow.library.data.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class EmailValidatorTest {

  @ValueSource(strings = {
      "formflow@gmail.com",
      "fce343@whis.edu",
  })
  @ParameterizedTest
  void validFormFlowEmailShouldReturnTrue(String validFormFlowEmail) {
    assertThat(new FormFlowEmailValidator().isValid(validFormFlowEmail, null)).isTrue();
  }

  @ValueSource(strings = {
      "test..book@gmail.com",
      ".hello@yahoo.org",
      "wha?tsssss23@.reatd?.com",
      "@book.com",
      "wakeup.com"
  })
  @ParameterizedTest
  void invalidFormFlowEmailShouldReturnFalse(String invalidFormFlowEmail) {
    assertThat(new FormFlowEmailValidator().isValid(invalidFormFlowEmail, null)).isFalse();
  }
}
