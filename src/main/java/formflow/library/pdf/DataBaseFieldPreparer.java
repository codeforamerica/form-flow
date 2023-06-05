package formflow.library.pdf;

import formflow.library.data.Submission;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataBaseFieldPreparer implements DefaultSubmissionFieldPreparer {

  @Override
  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap) {
    Map<String, SubmissionField> databaseFields = new HashMap<>();
    Map<String, Object> dbFields = pdfMap.getDbFields();

    dbFields.forEach((fieldName, value) -> {
      switch (fieldName) {
        case "submittedAt" ->
            databaseFields.put(fieldName, new DatabaseField(fieldName, formatDateWithNoTime(submission.getSubmittedAt())));
        case "submissionId" -> databaseFields.put(fieldName, new DatabaseField(fieldName, submission.getId().toString()));
        case "createdAt" ->
            databaseFields.put(fieldName, new DatabaseField(fieldName, formatDateWithNoTime(submission.getCreatedAt())));
        case "updatedAt" ->
            databaseFields.put(fieldName, new DatabaseField(fieldName, formatDateWithNoTime(submission.getUpdatedAt())));
        case "flow" -> databaseFields.put(fieldName, new DatabaseField(fieldName, submission.getFlow()));
        default -> log.error("Unable to map unknown database field: {}", fieldName);
      }
    });
    return databaseFields;
  }

  private String formatDateWithNoTime(Date date) {
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("MM/dd/yyyy")
        .withLocale(Locale.US)
        .withZone(java.time.ZoneId.systemDefault());
    return formatter.format(date.toInstant());
  }
}
