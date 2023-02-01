package formflow.library.data.validators;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MoneyValidator implements ConstraintValidator<Money, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Matches one or more digits, a dot and 2 digits after the dot.
    if (value.isEmpty()) {
      return false;
    } else {
      return Pattern.matches("(^(0|[1-9][0-9]*)\\.?\\d{1,2}$)?", value);
    }
  }
}
