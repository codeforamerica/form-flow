package formflow.library.config;

import lombok.Data;

/**
 * Configures a nested, repeated sub-iteration within a subflow's {@link SubflowRelationship}: for each value in
 * the input named {@code inputName}, a separate iteration is created and stored under {@code saveDataAs} in the
 * subflow entry's data.
 */
@Data
public class RepeatFor {

    private String inputName;
    private String saveDataAs;
    /**
     * Default constructor.
     */
    public RepeatFor() {
    }
}