package formflow.library.config;

import formflow.library.data.Submission;

public abstract class Condition {
  public abstract Boolean runCondition(Submission submission);
}
