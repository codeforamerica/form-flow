package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AggregateDatesInPI implements Action {

  public void run(FormSubmission formSubmission) {
    Map<String, Object> inputData = formSubmission.getFormData();

    String prefix = "date";
    List<String> dateComponents = new ArrayList<>(3);
    if (formSubmission.formData.containsKey(prefix + "Month") && formSubmission.formData.get(prefix + "Month") != "") {
      dateComponents.add((String) formSubmission.formData.get(prefix + "Month"));
      dateComponents.add((String) formSubmission.formData.get(prefix + "Day"));
      dateComponents.add((String) formSubmission.formData.get(prefix + "Year"));
      formSubmission.formData.put(prefix + "Full", String.join("/", dateComponents));
    }
  }
}
