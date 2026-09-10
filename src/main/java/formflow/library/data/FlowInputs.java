package formflow.library.data;


import jakarta.validation.constraints.NotBlank;

/**
 * Base class for a flow's declared input fields (e.g. {@code @Encrypted} markers used to identify which fields
 * need encryption at rest). A consuming application defines one subclass per flow, named to match the flow's
 * name (so {@code testFlow} maps to a class named {@code TestFlow}), which this library locates by reflection.
 */
public class FlowInputs {

    @NotBlank
    private String _csrf;

    /**
     * Default constructor.
     */
    public FlowInputs() {
    }
}
