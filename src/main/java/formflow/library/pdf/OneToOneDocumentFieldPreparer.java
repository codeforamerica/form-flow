package formflow.library.pdf;

import static formflow.library.pdf.DocumentFieldPreparer.formInputTypeToApplicationInputType;

import formflow.library.data.Submission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OneToOneDocumentFieldPreparer implements DocumentFieldPreparer {

  private final PdfMapConfiguration pdfMapConfiguration;

  public OneToOneDocumentFieldPreparer(PdfMapConfiguration pdfMapConfiguration) {
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  @Override
  public List<DocumentField> prepareDocumentFields(Submission submission) throws IOException {
    List<DocumentField> documentFields = new ArrayList<>();

    List<PdfMap> pdfMaps = pdfMapConfiguration.getMaps();

    pdfMapConfiguration.getPdfMap(submission.getFlow());

    submission.getInputData().forEach((key, value) -> {
      FormInputType formInputType = InputDataToDocumentFieldTypeConverter.getInputType(submission, key);
      if (formInputType == FormInputType.SINGLE_VALUE) {
        DocumentField documentField = new DocumentField(key, value.toString(), formInputTypeToApplicationInputType(formInputType),
            null);
        documentFields.add(documentField);
      }
    });
    return documentFields;
  }
}
