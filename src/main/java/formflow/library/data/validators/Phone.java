package formflow.library.data.validators;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented

public @interface Phone {

  String message() default "Please make sure you enter a ten digit phone number: (999) 999-9999";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}


