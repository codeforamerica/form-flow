package formflow.library.pdf;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PdfGenerator {

  private final SubmissionFieldPreparers submissionFieldPreparers;
  private final PdfFieldMapper pdfFieldMapper;
  private final PdfMapConfiguration pdfMapConfiguration;
  private final PDFBoxFieldFiller pdfBoxFieldFiller;

  public PdfGenerator(SubmissionFieldPreparers submissionFieldPreparers,
      PdfFieldMapper pdfFieldMapper, PdfMapConfiguration pdfMapConfiguration, PDFBoxFieldFiller pdfBoxFieldFiller) {
    this.submissionFieldPreparers = submissionFieldPreparers;
    this.pdfFieldMapper = pdfFieldMapper;
    this.pdfMapConfiguration = pdfMapConfiguration;
    this.pdfBoxFieldFiller = pdfBoxFieldFiller;
  }

  public PdfFile generate(String flow, UUID submissionId) {
    List<SubmissionField> submissionFields = submissionFieldPreparers.prepareSubmissionFields(submissionId);
    List<PdfField> pdfFields = pdfFieldMapper.map(submissionFields, flow);
    String pathToPdfResource = pdfMapConfiguration.getPdfFromFlow(flow);
    PdfFile tmpFile = pdfBoxFieldFiller.fill(pathToPdfResource, pdfFields);
    return tmpFile;
  }
}
