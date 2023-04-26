package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;

public class TestCustomPreparer implements SubmissionFieldPreparer {

  @Override
  public List<SubmissionField> prepareSubmissionFields(Submission submission) {
    return List.of(new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null));
  }
}
