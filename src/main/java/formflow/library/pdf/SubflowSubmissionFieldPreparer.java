package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.Map;

public interface SubflowSubmissionFieldPreparer {

  Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap);
}
