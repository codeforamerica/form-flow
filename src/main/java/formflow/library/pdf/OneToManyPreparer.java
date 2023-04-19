package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.List;

public class OneToManyPreparer implements SubmissionFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public OneToManyPreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public List<SubmissionField> prepareDocumentFields(Submission submission) {
    return List.of();
  }
}
