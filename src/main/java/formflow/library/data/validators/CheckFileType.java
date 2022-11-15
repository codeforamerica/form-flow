package formflow.library.data.validators;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = FileTypeValidator.class)
@Documented
public @interface CheckFileType {

  String message() default "{org.formflow.library.data.validators.CheckFileType.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
