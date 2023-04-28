package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.Map;

public interface DefaultSubmissionFieldPreparer {

  Map<String, SubmissionField> prepareSubmissionFields(Submission submission);
}
