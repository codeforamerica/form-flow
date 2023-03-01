package formflow.library.config.submission;

import formflow.library.data.FormSubmission;
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
   * @param id         id for the iteration
   */
  public default void run(Submission submission, String id) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * Runs an action on form submission data to potentially manipulate the data.
   *
   * @param formSubmission form submission object the action is associated with, not null
   */
  public default void run(FormSubmission formSubmission) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * Runs an action on form submission data to potentially manipulate the data.
   *
   * @param formSubmission form submission object the action is associated with, not null
   * @param data           id for the iteration
   */
  public default void run(FormSubmission formSubmission, String data) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
