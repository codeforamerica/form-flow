package formflow.library.pdf;

import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * A Checkbox Field
 */
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CheckboxField extends SubmissionField {

    @ToString.Include
    @EqualsAndHashCode.Include
    List<String> value;

    /**
     * A Checkbox Field
     * @param name Field name
     * @param value Field value
     * @param iteration Iteration
     */
    public CheckboxField(String name, List<String> value, Integer iteration) {
        super(name, iteration);
        this.value = value;
    }
}
