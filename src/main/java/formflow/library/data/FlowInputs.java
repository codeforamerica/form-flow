package formflow.library.data;


import jakarta.validation.constraints.NotBlank;

public class FlowInputs {

    @NotBlank
    private String _csrf;

    /**
     * Default constructor.
     */
    public FlowInputs() {
    }
}
