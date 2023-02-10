package formflow.library.data.validators;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FormFlowEmailValidator implements ConstraintValidator<FormFlowEmail, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Pattern.matches(
        "[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?",
        value);
  }
}