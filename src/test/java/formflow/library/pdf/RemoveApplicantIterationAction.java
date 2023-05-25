package formflow.library.pdf;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveApplicantIterationAction implements Action {

  public List<Map<String, Object>> runSubflowAction(Submission submission, PdfMapSubflow subflowMap) {
    List<Map<String, Object>> testSubflow = (List<Map<String, Object>>) submission.getInputData().get("testSubflow");
    return testSubflow.stream().filter(iteration -> !iteration.get("firstName").equals("Applicant")).collect(Collectors.toList());
  }
}
