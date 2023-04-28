package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;

public class TestCustomPreparer implements SubmissionFieldPreparer {

  @Override
  public HashMap<String, SubmissionField> prepareSubmissionFields(Submission submission) {
    HashMap<String, SubmissionField> testFieldMap = new HashMap<>();
    testFieldMap.put("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null));
    return testFieldMap;
  }
}
