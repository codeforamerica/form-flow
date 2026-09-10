package formflow.library.pdf;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link SubmissionField} whose value is a single, already-stringified value from a submission's input data -
 * the common case, prepared by {@link OneToOnePreparer} for a one-to-one input-to-PDF-field mapping, and by
 * {@link OneToManyPreparer}/{@link SubflowFieldPreparer} for individual checkbox/subflow values.
 */
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SingleField extends SubmissionField {

    /**
     * The field's value, already formatted as a string.
     */
    @ToString.Include
    @EqualsAndHashCode.Include
    @NotNull String value;

    /**
     * Creates a single field with an already-formatted value.
     *
     * @param name      the field's name, matching a key configured in a {@code pdf-map.yaml} file
     * @param value     the field's value, already formatted as a string
     * @param iteration the subflow iteration this field belongs to, or null if not part of a subflow
     */
    public SingleField(String name, @NotNull String value, Integer iteration) {
        super(name, iteration);
        this.value = value;
    }
}

