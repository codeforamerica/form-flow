package formflow.library.data.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The {@code DynamicField} annotation is used to mark a field as dynamic, as in we don't know how many of the fields will come
 * in.
 */
@Target({ElementType.FIELD, TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface DynamicField {

    /**
     * The default message that will be used in validation messages.
     *
     * @return the default validation message
     */
    String message() default "";

    /**
     * Defines the group(s) the constraint belongs to. This is used for grouped validation.
     *
     * @return the groups for which the constraint is applicable
     */
    Class<?>[] groups() default {};

    /**
     * Can be used by clients of the Bean Validation API to assign custom payload objects to a constraint.
     *
     * @return the payload associated with the constraint
     */
    Class<? extends Payload>[] payload() default {};

}
