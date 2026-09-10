package formflow.library.pdf;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link SubmissionField} whose value comes from a {@link formflow.library.data.Submission}'s own metadata
 * (e.g. {@code submittedAt}, {@code submissionId}) rather than from user-submitted input data. Prepared by
 * {@link DatabaseFieldPreparer}.
 */
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DatabaseField extends SubmissionField {

    /**
     * The field's value, already formatted as a string suitable for the PDF.
     */
    @ToString.Include
    @EqualsAndHashCode.Include
    @NotNull String value;

    /**
     * Creates a database field with an already-formatted value.
     *
     * @param name  the field's name, matching a key configured under {@code dbFields} in a {@code pdf-map.yaml} file
     * @param value the field's value, already formatted as a string
     */
    public DatabaseField(String name, @NotNull String value) {
        super(name, null);
        this.value = value;
    }

}
