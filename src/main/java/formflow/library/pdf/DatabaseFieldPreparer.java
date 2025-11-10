package formflow.library.pdf;

import formflow.library.data.Submission;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DatabaseFieldPreparer implements DefaultSubmissionFieldPreparer {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Default constructor.
     */
    public DatabaseFieldPreparer() {
    }

    @Override
    public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap) {
        Map<String, SubmissionField> databaseFields = new HashMap<>();
        Map<String, Object> dbFields = pdfMap.getDbFields();

        if (dbFields != null) {
            dbFields.forEach((fieldName, value) -> {
                switch (fieldName) {
                    case "submittedAt" -> databaseFields.put(fieldName,
                            new DatabaseField(fieldName, formatDateWithNoTime(submission.getSubmittedAt())));
                    case "submissionId" ->
                            databaseFields.put(fieldName, new DatabaseField(fieldName, submission.getId().toString()));
                    case "createdAt" -> databaseFields.put(fieldName,
                            new DatabaseField(fieldName, formatDateWithNoTime(submission.getCreatedAt())));
                    case "updatedAt" -> databaseFields.put(fieldName,
                            new DatabaseField(fieldName, formatDateWithNoTime(submission.getUpdatedAt())));
                    case "flow" -> databaseFields.put(fieldName, new DatabaseField(fieldName, submission.getFlow()));
                    default -> log.error("Unable to map unknown database field: {}", fieldName);
                }
            });
        }

        return databaseFields;
    }

    private String formatDateWithNoTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? dateTimeFormatter.format(offsetDateTime) : "";
    }
}
