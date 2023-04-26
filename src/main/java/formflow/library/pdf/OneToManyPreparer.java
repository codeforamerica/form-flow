package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OneToManyPreparer implements DefaultSubmissionFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public OneToManyPreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public List<SubmissionField> prepareSubmissionFields(Submission submission) {
    Map<String, Object> fieldMap = pdfMapConfiguration.getPdfMap(submission.getFlow()).getInputFields();
    return fieldMap.keySet().stream()
        .filter(field -> fieldMap.get(field) instanceof Map && submission.getInputData().get(field + "[]") != null)
        .map(
            field -> new CheckboxField(
                field,
                (List<String>) submission.getInputData().get(field + "[]"),
                null
            ))
        .collect(Collectors.toList());
  }
}
