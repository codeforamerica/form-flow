package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OneToManyPreparer implements DefaultSubmissionFieldPreparer {

  /**
   * Default constructor.
   */
  public OneToManyPreparer() {
  }

  @Override
  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap) {
    Map<String, Object> fieldMap = pdfMap.getAllFields();
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
