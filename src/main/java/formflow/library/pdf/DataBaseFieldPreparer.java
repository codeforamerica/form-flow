package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataBaseFieldPreparer implements SubmissionFieldPreparer {

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
        case "submittedAt" -> databaseFields.add(new DatabaseField(fieldName, submission.getSubmittedAt().toString()));
        case "submissionId" -> databaseFields.add(new DatabaseField(fieldName, submission.getId().toString()));
        case "createdAt" -> databaseFields.add(new DatabaseField(fieldName, submission.getCreatedAt().toString()));
        case "updatedAt" -> databaseFields.add(new DatabaseField(fieldName, submission.getUpdatedAt().toString()));
        case "flow" -> databaseFields.add(new DatabaseField(fieldName, submission.getFlow()));
        default -> log.error("Unable to map unknown database field: {}", fieldName);
      }
    });
    return databaseFields;
  }
}
