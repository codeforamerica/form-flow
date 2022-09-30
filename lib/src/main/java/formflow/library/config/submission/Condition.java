package formflow.library.config.submission;

import formflow.library.data.Submission;

/**
 * An interface that defines a function to run a Condition
 *
 * <p>
 *     Conditions are applied to screen flow, so each screen may have a condition (or multiple) attached to it.
 * </p>
 */
public interface Condition {

  /**
   * Runs a condition check on a submission.
   *
   * @param submission submission object the condition is associated with, not null
   * @return true if the condition check passes, else false
   */
  public default Boolean run(Submission submission) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * Runs a condition check on a submission's subflow iteration.
   *
   * @param submission  submission object the condition is associated with, not null
   * @param uuid  uuid of the subflow iteration this should operate on
   * @return true if the condition check passes, else false
   */
  public default Boolean run(Submission submission, String data) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
