package formflow.library.config.submission;

import formflow.library.data.Submission;

/**
 * An interface that defines a function to run a Condition
 *
 * <p>
 * Conditions are applied to screen flow, so each screen may have a condition (or multiple) attached to it.
 * </p>
 */
public interface Condition {

  /**
   * Runs a condition check on a submission.
   *
   * @param submission submission object the condition is associated with, not null
   * @return true if the condition check passes, else false
   */
  default Boolean run(Submission submission) {
    throw new UnsupportedOperationException("Method not implemented in " + this.getClass().getName());
  }

  /**
   * Runs a condition check on a submission's subflow iteration.
   *
   * @param submission  submission object the condition is associated with, not null
   * @param subflowUuid uuid of the subflow iteration this should operate on
   * @return true if the condition check passes, else false
   */
  default Boolean run(Submission submission, String subflowUuid) {
    throw new UnsupportedOperationException("Method not implemented in " + this.getClass().getName());
  }

  /**
   * Runs a condition check on a submission's subflow repeatFor iteration.
   *
   * @param submission     submission object the condition is associated with, not null
   * @param subflowUuid    uuid of the subflow iteration this should operate on
   * @param repeatForUuid uuid of the subflows's repeatFor iteration this should operate on
   * @return true if the condition check passes, else false
   */
  default Boolean run(Submission submission, String subflowUuid, String repeatForUuid) {
    throw new UnsupportedOperationException("Method not implemented in " + this.getClass().getName());
  }
}
