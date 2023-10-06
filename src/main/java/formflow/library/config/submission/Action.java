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
  default void run(Submission submission) {
    throw new UnsupportedOperationException("Not implemented in " + this.getClass().getName());
  }


  /**
   * Runs an action on a submission to potentially manipulate the data.
   *
   * @param submission submission object the action is associated with, not null
   * @param id         id for the iteration
   */
  default void run(Submission submission, String id) {
    throw new UnsupportedOperationException("Not implemented in " + this.getClass().getName());
  }

  /**
   * Runs an action on form submission and submission data to potentially manipulate the data.
   *
   * @param formSubmission form submission object the action is associated with, not null
   * @param submission     submission object the action is associated with, not null
   */
  default void run(FormSubmission formSubmission, Submission submission) {
    throw new UnsupportedOperationException("Not implemented in " + this.getClass().getName());
  }

  /**
   * Runs an action on form submission and submission data to potentially manipulate the data.
   *
   * @param formSubmission form submission object the action is associated with, not null
   * @param submission     submission object the action is associated with, not null
   * @param id             id for the iteration
   */
  default void run(FormSubmission formSubmission, Submission submission, String id) {
    throw new UnsupportedOperationException("Not implemented " + this.getClass().getName());
  }

  /**
   * Runs validation code with the expectation that error messages may be returned.
   *
   * @param formSubmission form submission object the action is associated with, not null
   * @deprecated use `runValidation(final FormSubmission formSubmission, Submission submission)` instead.
   * @return a hashmap of a String to List of Strings that represents errors and their corresponding error messages.
   */
  @Deprecated
  default Map<String, List<String>> runValidation(final FormSubmission formSubmission) {
    throw new UnsupportedOperationException("Not implemented in " + this.getClass().getName());
  }


  /**
   * Runs validation code with the expectation that error messages may be returned.
   *
   * @param formSubmission form submission object the action is associated with, not null
   * @param submission     submission object the action is associated with, not null
   * @return a hashmap of a String to List of Strings that represents errors and their corresponding error messages.
   */
  default Map<String, List<String>> runValidation(final FormSubmission formSubmission, Submission submission) {
    throw new UnsupportedOperationException("Not implemented in " + this.getClass().getName());
  }
}
