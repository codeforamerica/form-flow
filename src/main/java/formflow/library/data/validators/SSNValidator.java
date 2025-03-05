package formflow.library.data.validators;

import formflow.library.data.annotations.SSN;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that a given string is in the correct social security number format. This validator is used in conjunction with the
 * {@link formflow.library.data.annotations.SSN} annotation to ensure that the string representation of SSN values adheres to this format.
 * <p>
 * The default format is defined as ###-##-####, but with the following constraints:
 * <p>
 * does not begin with 000, 666, or 900-999
 * does not have 00 in the middle group
 * does not end with 0000
 */
@Component
@Slf4j
public class SSNValidator implements ConstraintValidator<SSN, String> {

    @Value("${form-flow.validation.ssn-pattern:^(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}$}")
    private String pattern;

    /**
     * Checks if the provided {@code String} value matches the expected SSN format. T
     *
     * @param value   the {@code String} value to be validated
     * @param context context in which the constraint is evaluated
     * @return {@code true} if the value matches the SSN format, otherwise {@code false}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        log.info(value + " " + pattern);

        if (value != null && !value.isBlank()) {
            return Pattern.matches(pattern, value);
        }
        return true;
    }
}
