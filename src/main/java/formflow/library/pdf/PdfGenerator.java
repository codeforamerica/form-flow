package formflow.library.pdf;

import formflow.library.data.Submission;
import org.springframework.stereotype.Component;

import java.util.List;

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

  /**
   * Generates a PdfFile based on Submission data and a certain Form Flow
   *
   * @param flow       the form flow we are working with
   * @param submission the submission we are going to map the data of
   * @return A PdfFile which contains the path to the newly created and filled in PDF file.
   */
  public PdfFile generate(String flow, Submission submission) {
    List<SubmissionField> submissionFields = submissionFieldPreparers.prepareSubmissionFields(submission);
    List<PdfField> pdfFields = pdfFieldMapper.map(submissionFields, flow);
    String pathToPdfResource = pdfMapConfiguration.getPdfPathFromFlow(flow);
    PdfFile tmpFile = pdfBoxFieldFiller.fill(pathToPdfResource, pdfFields);
    return tmpFile;
  }
}
