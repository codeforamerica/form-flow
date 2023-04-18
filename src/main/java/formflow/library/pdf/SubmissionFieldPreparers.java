package formflow.library.pdf;

import static formflow.library.pdf.SubmissionFieldType.SINGLE_VALUE;

import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
    List<SubmissionField> fields = new ArrayList<>(getDefaultFields(submission));

    // Run all the preparers
    preparers.forEach(preparer -> {
      try {
        fields.addAll(preparer.prepareDocumentFields(submission));
      } catch (Exception e) {
        String preparerClassName = preparer.getClass().getSimpleName();
        log.error("There was an issue preparing submission data for " + preparerClassName, e);
      }
    });

    return fields;
  }

  @NotNull
  private List<SubmissionField> getDefaultFields(Submission submission) {
    return List.of(
        new SubmissionField("submittedAt", String.valueOf(submission.getSubmittedAt()), SINGLE_VALUE, null),
        new SubmissionField("submissionId", String.valueOf(submission.getId()), SINGLE_VALUE, null)
    );
  }
}
