package formflow.library.data.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyValidatorTest {

  @ValueSource(strings = {
      "100",
      "100.05",
      ".05",
  })
  @ParameterizedTest
  void validMoneyAmountsShouldReturnTrue(String value) {
    assertThat(new MoneyValidator().isValid(value, null)).isTrue();
  }

  @ValueSource(strings = {
      "100.1",
      "012.34",
      "0999",
      "-100",
      "100.123",
  })
  @ParameterizedTest
  void invalidMoneyAmountsShouldReturnFalse(String value) {
    assertThat(new MoneyValidator().isValid(value, null)).isFalse();
  }
}