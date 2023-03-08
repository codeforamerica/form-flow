package formflow.library.config.submission;

import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;

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
   * @param id             id for the iteration
   */
  public default void run(FormSubmission formSubmission, String id) {
    throw new UnsupportedOperationException("Not implemented run Validation");
  }

  /*
   * Runs validation code with the expectation that error messages may be returned.
   */
  public default Map<String, List<String>> runValidation(final FormSubmission formSubmission) {
    throw new UnsupportedOperationException("Not implemented");
  }

}
