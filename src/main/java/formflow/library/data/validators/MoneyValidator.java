package formflow.library.data.validators;

import formflow.library.data.annotations.Money;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a given string is in the correct money format. The format is defined as one or more digits, optionally followed
 * by a dot and exactly two digits after the dot. This validator is used in conjunction with the {@link Money} annotation to
 * ensure that the string representation of money values adheres to this format.
 */
public class MoneyValidator implements ConstraintValidator<Money, String> {

  /**
   * Checks if the provided {@code String} value matches the expected money format. The format is validated against the regex
   * pattern which ensures that the value consists of one or more digits, optionally followed by a dot and two decimal places.
   *
   * @param value   the {@code String} value to be validated
   * @param context context in which the constraint is evaluated
   * @return {@code true} if the value matches the money format, otherwise {@code false}
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Pattern.matches("(^(0|([1-9]\\d*))?(\\.\\d{1,2})?$)?", value);
  }
}
