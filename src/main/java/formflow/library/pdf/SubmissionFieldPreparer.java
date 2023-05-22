package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.Map;

public interface SubmissionFieldPreparer {

  Map<String, SubmissionField> prepareSubmissionFields(Submission submission, Map<String, Object> data, PdfMap pdfMap);
}
