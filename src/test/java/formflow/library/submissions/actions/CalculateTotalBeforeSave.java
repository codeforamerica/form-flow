package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class CalculateTotalBeforeSave implements Action {

  public void run(Submission submission, String _id) {
    List<Map<String, Object>> subflow = (List<Map<String, Object>>) submission.getInputData()
        .get("income");
    var totalIncome = subflow.stream()
        .map(e -> Double.parseDouble((String) e.get("textInput")))
        .reduce(Double::sum)
        .get();

    submission.getInputData().put("totalIncome", totalIncome);
  }
}
