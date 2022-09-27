package formflow.library.config;

import formflow.library.data.Submission;

/**
 * An abstract class that defines a function to run a Condition
 *
 * <p>
 *     Conditions are applied to screen flow, so each screen may have a condition (or multiple) attached to it.
 * </p>
 */
public abstract class Condition {

  /**
   * Runs a condition check.
   *
   * @param submission submission object the condition is associated with, not null
   * @return true if the condition check passes, else false
   */
  public abstract Boolean runCondition(Submission submission);
}
