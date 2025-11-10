package formflow.library.data.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import formflow.library.data.validators.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Custom annotation for validating phone numbers in form submissions. This annotation ensures that the annotated field contains a
 * valid phone number in a specified format. It uses {@link PhoneValidator} for the validation logic.
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface Phone {

    /**
     * Default message that will be used when the phone number validation fails.
     *
     * @return The default error message.
     */
    String message() default "Please make sure you enter a ten digit phone number: (999) 999-9999";

    /**
     * Optional groups for categorizing validation constraints.
     *
     * @return An array of group classes.
     */
    Class<?>[] groups() default {};

    /**
     * Can be used by clients of the Bean Validation API to assign custom payload objects to a constraint.
     *
     * @return An array of payload classes.
     */
    Class<? extends Payload>[] payload() default {};
}


