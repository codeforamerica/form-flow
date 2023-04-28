package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OneToOnePreparer implements DefaultSubmissionFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public OneToOnePreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission) {
    Map<String, Object> fieldMap = pdfMapConfiguration.getPdfMap(submission.getFlow()).getInputFields();
    Map<String, SubmissionField> preppedFields = new HashMap<>();
    fieldMap.keySet().stream()
        .filter(field -> (fieldMap.get(field) instanceof String) && (submission.getInputData().get(field) != null))
        .forEach(field ->
            preppedFields.put(field, new SingleField(
                    field,
                    submission.getInputData().get(field).toString(),
                    null
                )
            )
        );

    return preppedFields;
  }
}
