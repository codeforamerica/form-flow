package formflow.library.data.validators;

import formflow.library.data.annotations.Phone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator class for custom {@link Phone} annotation. This validator checks if the input string is a valid phone number matching
 * a specific format.
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

  /**
   * Validates the given phone number against a predefined pattern. This pattern corresponds to a standard US phone number.
   * format.
   *
   * @param value   The phone number to validate.
   * @param context Context in which the constraint is evaluated.
   * @return {@code true} if the phone number is valid.
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value != null) {
      return Pattern.matches("(\\([2-9][0-8][0-9]\\)\\s\\d{3}-\\d{4})", value);
    }
    return true;
  }
}
