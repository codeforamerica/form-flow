package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.Map;
import java.util.UUID;

public interface DefaultSubmissionFieldPreparer {

  Map<String, SubmissionField> prepareSubmissionFields(Submission submission, Map<String, Object> data, PdfMap pdfMap);
}
