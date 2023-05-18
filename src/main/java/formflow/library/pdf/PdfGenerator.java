package formflow.library.pdf;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PdfGenerator {

  private final SubmissionRepositoryService submissionRepositoryService;
  private final SubmissionFieldPreparers submissionFieldPreparers;
  private final PdfFieldMapper pdfFieldMapper;
  private final PdfMapConfiguration pdfMapConfiguration;
  private final PDFBoxFieldFiller pdfBoxFieldFiller;

  public PdfGenerator(SubmissionRepositoryService submissionRepositoryService, SubmissionFieldPreparers submissionFieldPreparers,
      PdfFieldMapper pdfFieldMapper, PdfMapConfiguration pdfMapConfiguration, PDFBoxFieldFiller pdfBoxFieldFiller) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.submissionFieldPreparers = submissionFieldPreparers;
    this.pdfFieldMapper = pdfFieldMapper;
    this.pdfMapConfiguration = pdfMapConfiguration;
    this.pdfBoxFieldFiller = pdfBoxFieldFiller;
  }

  public PdfFile generate(String flow, UUID id) {
    Submission submission = submissionRepositoryService.findById(id).orElseThrow();
    List<SubmissionField> submissionFields = submissionFieldPreparers.prepareSubmissionFields(submission);
    List<PdfField> pdfFields = pdfFieldMapper.map(submissionFields, flow);
    String pathToPdfResource = pdfMapConfiguration.getPdfFromFlow(flow);
    PdfFile tmpFile = pdfBoxFieldFiller.fill(pathToPdfResource, pdfFields);
    return tmpFile;
  }
}
