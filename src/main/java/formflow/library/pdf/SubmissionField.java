package formflow.library.pdf;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public abstract class SubmissionField {

    @ToString.Include
    @EqualsAndHashCode.Include
    public String name = null;
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
