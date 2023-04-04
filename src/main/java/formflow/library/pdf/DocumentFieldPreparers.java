package formflow.library.pdf;

import static formflow.library.pdf.DocumentFieldType.SINGLE_VALUE;

import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DocumentFieldPreparers {

  private final List<DocumentFieldPreparer> preparers;

  public DocumentFieldPreparers(List<DocumentFieldPreparer> preparers) {
    this.preparers = preparers;
  }

  public List<DocumentField> prepareDocumentFields(Submission submission) {

    // Add default fields
    List<DocumentField> fields = new ArrayList<>(getDefaultFields(submission));

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
  private List<DocumentField> getDefaultFields(Submission submission) {
    return List.of(
        new DocumentField("submissionId", String.valueOf(submission.getId()),
            SINGLE_VALUE, null)
    );
  }
}
