package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;

public interface SubmissionFieldPreparer {

  List<SubmissionField> prepareSubmissionFields(Submission submission);
}
