package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DataBaseFieldPreparer implements SubmissionFieldPreparer {

  @Override
  public List<SubmissionField> prepareSubmissionFields(Submission submission) {
    return submission.getSubmittedAt() != null ?
        List.of(new DatabaseField("submittedAt", submission.getSubmittedAt().toString())) :
        List.of();
  }
}
