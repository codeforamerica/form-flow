package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class FormatDateBeforeSave implements Action {

  public void run(Submission submission) {
    List<String> dateComponents = new ArrayList<>(3);
    dateComponents.add((String) submission.getInputData().get("dateMonth"));
    dateComponents.add((String) submission.getInputData().get("dateDay"));
    dateComponents.add((String) submission.getInputData().get("dateYear"));
    submission.getInputData().put("formattedDate", String.join("/", dateComponents));
  }
}
