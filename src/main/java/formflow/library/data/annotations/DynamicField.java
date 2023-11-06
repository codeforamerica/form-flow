package formflow.library.data.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface DynamicField {

  String message() default "";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

}
