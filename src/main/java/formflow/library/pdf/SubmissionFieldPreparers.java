package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubmissionFieldPreparers {

  private final List<DefaultSubmissionFieldPreparer> defaultPreparers;

  private final List<SubmissionFieldPreparer> customPreparers;

  public SubmissionFieldPreparers(List<DefaultSubmissionFieldPreparer> defaultPreparers,
      List<SubmissionFieldPreparer> customPreparers) {
    this.defaultPreparers = defaultPreparers;
    this.customPreparers = customPreparers;
  }

  public List<SubmissionField> prepareSubmissionFields(Submission submission) {

    HashMap<String, SubmissionField> submissionFieldsMap = new HashMap<>();

    defaultPreparers.forEach(preparer -> {
      try {
        submissionFieldsMap.putAll(preparer.prepareSubmissionFields(submission));
      } catch (Exception e) {
        String preparerClassName = preparer.getClass().getSimpleName();
        log.error("There was an issue preparing submission data for " + preparerClassName, e);
      }
    });

    customPreparers.forEach(preparer -> {
      try {
        submissionFieldsMap.putAll(preparer.prepareSubmissionFields(submission));
      } catch (Exception e) {
        String preparerClassName = preparer.getClass().getSimpleName();
        log.error("There was an issue preparing submission data for " + preparerClassName, e);
      }
    });

    return submissionFieldsMap.values().stream().toList();
  }
}
