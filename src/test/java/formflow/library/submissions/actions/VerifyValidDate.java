package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("unused")
public class VerifyValidDate implements Action {
  public static final DateTimeFormatter DTF = DateTimeFormat.forPattern("MM/dd/yyyy");
  public static final DateTime MIN_DATE = DTF.parseDateTime("01/01/1901");

  public Map<String, List<String>> runValidation(FormSubmission formSubmission, Submission submission) {
    Map<String, Object> inputData = formSubmission.getFormData();
    String parentDate = String.format("%s/%s/%s",
        inputData.get("dateMonth"),
        inputData.get("dateDay"),
        inputData.get("dateYear"));

    if (parentDate.equals("null/null/null")) {
      return Collections.emptyMap();
    }

    if (!isDateValid(parentDate)) {
      return Map.of("dateFull", List.of("Please enter a valid date"));
    }
    if (!isBetweenNowAndMinDate(parentDate)) {
      return Map.of("dateFull", List.of("Make sure you enter a date between 01/01/1901 and now"));
    }

    return Collections.emptyMap();
  }

  private boolean isDateValid(String date) {
    try {
      DTF.parseDateTime(date);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isBetweenNowAndMinDate(String dateAsString) {
    try {
      DateTime date = DTF.parseDateTime(dateAsString);
      return MIN_DATE.isBefore(date.getMillis()) && date.isBeforeNow();
    } catch (Exception e) {
      return false;
    }
  }
}
