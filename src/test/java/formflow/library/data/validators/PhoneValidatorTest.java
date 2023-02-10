package formflow.library.data.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PhoneValidatorTest {

  @ValueSource(strings = {
      "(333) 333-3333",
      "(923) 456-7890",
      "(823) 456-7890",
      "(707) 987-5266",
      "(829) 622-9048"
  })
  @ParameterizedTest
  void validPhoneNumberShouldReturnTrue(String validPhoneNumber) {
    assertThat(new PhoneValidator().isValid(validPhoneNumber, null)).isTrue();
  }

  @ValueSource(strings = {
      "+1(111)-1111",
      "(111)222-3333",
      "(111)111-11",
      "999999999",
      "(123) 456-7890",
      "(077) 987-5266",
      "(892) 622-9048",
      "22",
      "der"
  })
  @ParameterizedTest
  void invalidPhoneNumberShouldReturnFalse(String invalidPhoneNumber) {
    assertThat(new PhoneValidator().isValid(invalidPhoneNumber, null)).isFalse();
  }
}
