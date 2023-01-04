package formflow.library.data.validators;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MoneyValidator implements ConstraintValidator<Money, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Pattern.matches("([1-9]\\d*)?(\\.\\d{2})?", value);
  }
}
