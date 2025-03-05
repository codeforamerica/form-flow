package formflow.library.data.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import formflow.library.data.validators.SSNValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Custom annotation for validating social security number values. This annotation can be used on fields to ensure that they
 * contain a valid representation of a SSN. It uses {@link formflow.library.data.validators.SSNValidator} for the validation
 * logic.
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = SSNValidator.class)
@Documented
public @interface SSN {

    /**
     * Default message to be used in validation failure.
     *
     * @return The default error message.
     */
    String message() default "Make sure the SSN is valid and 9 digits";

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
