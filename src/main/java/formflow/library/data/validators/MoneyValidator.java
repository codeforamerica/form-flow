package formflow.library.data.validators;

import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * This class validates that Money has been passed in the correct format - one or more digits, a dot and 2 digits after the dot.
 */
public class MoneyValidator implements ConstraintValidator<Money, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Pattern.matches("(^(0|([1-9]\\d*))?(\\.\\d{1,2})?$)?", value);
  }
}
