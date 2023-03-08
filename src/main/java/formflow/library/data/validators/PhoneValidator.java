package formflow.library.data.validators;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Pattern.matches("(\\([2-9][0-8][0-9]\\)\\s\\d{3}-\\d{4})", value);
  }
}
