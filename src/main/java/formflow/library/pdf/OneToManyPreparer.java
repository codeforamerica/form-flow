package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OneToManyPreparer implements DefaultSubmissionFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public OneToManyPreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission) {
    Map<String, Object> fieldMap = pdfMapConfiguration.getPdfMap(submission.getFlow()).getInputFields();
    Map<String, SubmissionField> preppedFields = new HashMap<>();

    fieldMap.keySet().stream()
        .filter(field -> fieldMap.get(field) instanceof Map && submission.getInputData().get(field + "[]") != null)
        .forEach(field ->
            preppedFields.put(field, new CheckboxField(
                    field,
                    (List<String>) submission.getInputData().get(field + "[]"),
                    null
                )
            )
        );
    return preppedFields;
  }
}
