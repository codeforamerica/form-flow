package formflow.library.pdf;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Base type for a single field's data as prepared for filling in a submission's PDF, keyed by {@link #getName()}.
 * Subtypes ({@link SingleField}, {@link DatabaseField}) add the actual value; this class holds the name/subflow
 * iteration a field belongs to.
 */
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public abstract class SubmissionField {

    /**
     * The field's base name, before any subflow iteration suffix is applied by {@link #getName()}.
     */
    @ToString.Include
    @EqualsAndHashCode.Include
    public String name = null;
    /**
     * The subflow iteration this field belongs to, or null if the field isn't part of a subflow.
     */
    @ToString.Include
    @EqualsAndHashCode.Include
    @Getter
    public Integer iteration = null;

    /**
     * Default constructor.
     */
    public SubmissionField() {
    }

    /**
     * Returns the name of the field. If the field is part of a subflow, the name will be the field name suffixed with a "_" and
     * iteration number.
     * <br>
     * Given this data:
     * <pre>
     *    name = "incomeJob"
     *    iteration = 2
     * </pre>
     * This method would return: `incomeJob_2`.
     * <br>
     * If no iteration value is set, then just the name is returned:
     * <pre>
     *    name = "firstName"
     *    iteration = null
     * </pre>
     * This method would return: `firstName`
     *
     * @return name of field
     */
    public String getName() {
        return iteration != null ? name + "_" + iteration : name;
    }
}
