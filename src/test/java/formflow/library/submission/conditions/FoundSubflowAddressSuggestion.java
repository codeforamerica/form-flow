package formflow.library.submission.conditions;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import java.util.Map;

public class FoundSubflowAddressSuggestion implements Condition {

  @Override
  public Boolean run(Submission submission) {
    Map<String, Object> subflowData = (Map<String, Object>) submission.getInputData().get("testSubflow");
    return subflowData.containsKey("validationOnStreetAddress1_validated");
  }
}
