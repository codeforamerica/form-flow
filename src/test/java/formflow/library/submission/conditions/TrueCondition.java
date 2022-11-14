package formflow.library.submission.conditions;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;

@SuppressWarnings("unused")
public class TrueCondition implements Condition {

  @Override
  public Boolean run(Submission submission) {
    return true;
  }
}
