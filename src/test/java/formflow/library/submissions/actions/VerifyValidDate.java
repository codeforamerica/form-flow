package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("unused")
public class VerifyValidDate implements Action {
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("M/d/yyyy");
  public static final LocalDate MIN_DATE = LocalDate.parse("01/01/1901", DTF);

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
      LocalDate.parse(date, DTF);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private boolean isBetweenNowAndMinDate(String dateAsString) {
    try {
      LocalDate date = LocalDate.parse(dateAsString, DTF);
      return !date.isBefore(MIN_DATE) && !date.isAfter(LocalDate.now());
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
