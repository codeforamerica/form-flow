package formflow.library.pdf;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveApplicantIterationAction implements Action {

  /**
   * A test action that removes the applicant iteration from the subflow.
   *
   * @param submission The submission object.
   * @param subflowMap The subflow map object.
   * @return If the testSubflow contains an iteration with the firstName "Applicant", it will be removed from the subflow list and
   * the remaining iterations will be returned.
   */
  public List<Map<String, Object>> runSubflowAction(Submission submission, PdfMapSubflow subflowMap) {
    List<Map<String, Object>> testSubflow = (List<Map<String, Object>>) submission.getInputData().get("testSubflow");
    return testSubflow.stream().filter(iteration -> !iteration.get("firstName").equals("Applicant")).collect(Collectors.toList());
  }
}
