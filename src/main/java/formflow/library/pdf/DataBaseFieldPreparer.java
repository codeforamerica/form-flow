package formflow.library.pdf;

import formflow.library.data.Submission;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataBaseFieldPreparer implements DefaultSubmissionFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public DataBaseFieldPreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public List<SubmissionField> prepareSubmissionFields(Submission submission) {
    List<SubmissionField> databaseFields = new ArrayList<>();
    Map<String, Object> dbFields = pdfMapConfiguration.getPdfMap(submission.getFlow()).getDbFields();

    dbFields.forEach((fieldName, value) -> {
      switch (fieldName) {
        case "submittedAt" -> databaseFields.add(new DatabaseField(fieldName, formatDateWithNoTime(submission.getSubmittedAt())));
        case "submissionId" -> databaseFields.add(new DatabaseField(fieldName, submission.getId().toString()));
        case "createdAt" -> databaseFields.add(new DatabaseField(fieldName, formatDateWithNoTime(submission.getCreatedAt())));
        case "updatedAt" -> databaseFields.add(new DatabaseField(fieldName, formatDateWithNoTime(submission.getUpdatedAt())));
        case "flow" -> databaseFields.add(new DatabaseField(fieldName, submission.getFlow()));
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
