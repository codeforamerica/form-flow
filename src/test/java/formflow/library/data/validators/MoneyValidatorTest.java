package formflow.library.data.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyValidatorTest {

  @ValueSource(strings = {
      "100", "100.05", "0.05", "0.15", "0.1", "100.1", "1", "9", ".1", ""
  })
  @ParameterizedTest
  void validMoneyAmountsShouldReturnTrue(String value) {
    assertThat(new MoneyValidator().isValid(value, null)).isTrue();
  }

  @ValueSource(strings = {
      "012.34",
      "0999",
      "-100",
      "100.123", "1.2.3.4", "01.5"
  })
  @ParameterizedTest
  void invalidMoneyAmountsShouldReturnFalse(String value) {
    assertThat(new MoneyValidator().isValid(value, null)).isFalse();
  }
}