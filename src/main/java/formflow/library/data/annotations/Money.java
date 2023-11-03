package formflow.library.data.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import formflow.library.data.validators.MoneyValidator;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = MoneyValidator.class)
@Documented
public @interface Money {

  String message() default "Please make sure to enter a valid dollar amount. Example: 1.50.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
