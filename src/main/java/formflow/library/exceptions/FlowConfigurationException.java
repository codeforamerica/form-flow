package formflow.library.exceptions;

/**
 * Custom exception for handling configuration errors related to form flows. This exception is thrown when there are issues in the
 * configuration of the form flows, such as missing or invalid configurations that are essential for the flow's operation.
 */
public class FlowConfigurationException extends RuntimeException {

    /**
     * Constructs a new FlowConfigurationException with the specified message.
     *
     * @param message The message that is sent on exception
     */
    public FlowConfigurationException(String message) {
        super(message);
    }
}
