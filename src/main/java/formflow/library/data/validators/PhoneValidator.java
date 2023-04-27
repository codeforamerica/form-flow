package formflow.library.data.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Pattern.matches("(\\([2-9][0-8][0-9]\\)\\s\\d{3}-\\d{4})", value);
  }
}
