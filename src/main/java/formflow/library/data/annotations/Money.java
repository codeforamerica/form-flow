package formflow.library.data.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import formflow.library.data.validators.MoneyValidator;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom annotation for validating monetary values. This annotation can be used on fields to ensure that they contain a valid
 * representation of a monetary amount. It uses {@link MoneyValidator} for the validation logic.
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = MoneyValidator.class)
@Documented
public @interface Money {

  /**
   * Default message to be used in validation failure.
   *
   * @return The default error message.
   */
  String message() default "Please make sure to enter a valid dollar amount. Example: 1.50.";

  /**
   * Defines the group(s) the constraint belongs to. This is used for grouped validation.
   *
   * @return The groups to which this constraint belongs.
   */
  Class<?>[] groups() default {};

  /**
   * Can be used by clients of the Bean Validation API to assign custom payload objects to a constraint.
   *
   * @return The payload associated with the constraint.
   */
  Class<? extends Payload>[] payload() default {};
}
