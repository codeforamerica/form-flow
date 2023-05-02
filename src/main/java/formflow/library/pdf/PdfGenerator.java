package formflow.library.pdf;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class PdfGenerator {

  private final SubmissionRepositoryService submissionRepositoryService;
  private final SubmissionFieldPreparers submissionFieldPreparers;
  private final PdfFieldMapper pdfFieldMapper;
  private final PdfMapConfiguration pdfMapConfiguration;

  public PdfGenerator(SubmissionRepositoryService submissionRepositoryService, SubmissionFieldPreparers submissionFieldPreparers,
      PdfFieldMapper pdfFieldMapper, PdfMapConfiguration pdfMapConfiguration) {
    this.submissionRepositoryService = submissionRepositoryService;
    this.submissionFieldPreparers = submissionFieldPreparers;
    this.pdfFieldMapper = pdfFieldMapper;
    this.pdfMapConfiguration = pdfMapConfiguration;
  }

  public PdfFile generate(String flow, UUID id) throws IOException {
    Submission submission = submissionRepositoryService.findById(id).orElseThrow();
    List<SubmissionField> submissionFields = submissionFieldPreparers.prepareSubmissionFields(submission);
    List<PdfField> pdfFields = pdfFieldMapper.map(submissionFields, flow);
    PdfFile emptyFile = pdfMapConfiguration.getPdfFromFlow(flow);
    PDDocument filledPdf = new PDFBoxFieldFiller(List.of(new ByteArrayResource(emptyFile.fileBytes()))).fill(pdfFields,
            emptyFile.fileName());
    return new PdfFile(filledPdf, emptyFile.fileName());
  }
}
