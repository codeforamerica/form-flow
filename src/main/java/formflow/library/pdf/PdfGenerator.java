package formflow.library.pdf;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

@Component
public class PdfGenerator {

  private final SubmissionRepositoryService submissionRepositoryService;
  private final DocumentFieldPreparers documentFieldPreparers;
  private final PdfFieldMapper pdfFieldMapper;

  public PdfGenerator(SubmissionRepositoryService submissionRepositoryService, DocumentFieldPreparers documentFieldPreparers,
      PdfFieldMapper pdfFieldMapper) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.documentFieldPreparers = documentFieldPreparers;
    this.pdfFieldMapper = pdfFieldMapper;
  }

  public ApplicationFile generate(ApplicationFile blankFile, UUID id) {
    Submission submission = submissionRepositoryService.findById(id).orElseThrow();
    List<DocumentField> documentFields = documentFieldPreparers.prepareDocumentFields(submission);
    List<PdfField> pdfFields = pdfFieldMapper.map(documentFields, blankFile.fileName());
    ApplicationFile filledFile = new PDFBoxFieldFiller(List.of(new ByteArrayResource(blankFile.fileBytes()))).fill(pdfFields,
        blankFile.fileName());

    return filledFile;
  }

}
