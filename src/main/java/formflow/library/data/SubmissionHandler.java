package formflow.library.data;

import java.util.ArrayList;
import java.util.Map;

public class SubmissionHandler {

  private Submission submission;

  public SubmissionHandler() {
    submission = null;
  }

  public SubmissionHandler(Submission submission) {
    this.submission = submission;
  }

  public void setSubmission(Submission submission) {
    this.submission = submission;
  }

  // may not need or want to expose??
  public Map<String, Object> getInputData() {
    return submission.getInputData();
  }

  public Boolean submissionContains(String name) {
    return submission.getInputData().containsKey(name);
  }

  public ArrayList<Map<String, Object>> getSubflowData(String subflowName) {
    if (submission != null && submission.getInputData().containsKey(subflowName)) {
      return (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);
    }
    return null;
  }
}
