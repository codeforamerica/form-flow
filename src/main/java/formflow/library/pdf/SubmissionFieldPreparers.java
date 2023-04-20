package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubmissionFieldPreparers {

  private final List<SubmissionFieldPreparer> preparers;

  public SubmissionFieldPreparers(List<SubmissionFieldPreparer> preparers) {
    this.preparers = preparers;
  }

  public List<SubmissionField> prepareSubmissionFields(Submission submission) {

    // Add default fields
    List<SubmissionField> fields = new ArrayList<>();

    // Run all the preparers
    preparers.forEach(preparer -> {
      try {
        fields.addAll(preparer.prepareSubmissionFields(submission));
      } catch (Exception e) {
        String preparerClassName = preparer.getClass().getSimpleName();
        log.error("There was an issue preparing submission data for " + preparerClassName, e);
      }
    });
    return fields;
  }
}
