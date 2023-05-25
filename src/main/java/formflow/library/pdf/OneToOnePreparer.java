package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OneToOnePreparer implements DefaultSubmissionFieldPreparer {

  @Override
  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, Map<String, Object> data, PdfMap pdfMap) {
    Map<String, SubmissionField> preppedFields = new HashMap<>();
    Map<String, Object> fieldMap = pdfMap.getAllFields();

    fieldMap.keySet().stream()
        .filter(field -> (fieldMap.get(field) instanceof String) && (data.get(field) != null))
        .forEach(field ->
            preppedFields.put(field, new SingleField(
                    field,
                    data.get(field).toString(),
                    null
                )
            )
        );

    return preppedFields;
  }
}
