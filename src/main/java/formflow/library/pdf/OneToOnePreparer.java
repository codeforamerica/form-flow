package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OneToOnePreparer implements SubmissionFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public OneToOnePreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public List<SubmissionField> prepareDocumentFields(Submission submission) {
    Map<String, Object> fieldMap = pdfMapConfiguration.getPdfMap(submission.getFlow()).getInputs();
    return fieldMap.keySet().stream().filter(field -> fieldMap.get(field) instanceof String).map(
        field -> new SubmissionField(
            field,
            submission.getInputData().get(field).toString(),
            SubmissionFieldValue.SINGLE_FIELD, null
        )).collect(Collectors.toList());
  }
}
