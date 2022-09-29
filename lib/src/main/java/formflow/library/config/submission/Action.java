package formflow.library.config.submission;

import formflow.library.data.Submission;

/**
 * An interface to define a particular Action.
 */
public interface Action {

    /**
     * Runs an action on a submission to potentially manipulate the data.
     *
     * @param submission submission object the action is associated with, not null
     */
    public default void run(Submission submission) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Runs an action on a submission to potentially manipulate the data.
     *
     * @param submission submission object the action is associated with, not null
     * @param uuid id for the iteration
     */
    public default void run(Submission submission, String data) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
