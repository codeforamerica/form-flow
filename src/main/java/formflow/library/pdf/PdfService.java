package formflow.library.pdf;

import formflow.library.data.Submission;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PdfService {

  private final PdfGenerator pdfGenerator;

  /**
   * <b>PdfService</b> is a service that generates a byte[] of a flattened pdf.
   */
  public PdfService(PdfGenerator pdfGenerator) {
    this.pdfGenerator = pdfGenerator;
  }

  /**
   * Uses pdfGenerator to generate a pdf for a flow using a UUID.
   *
   * @param submission the submission for which the PDF should be generated
   * @return a pdf byte array
   */
  public byte[] getFilledOutPDF(Submission submission) throws IOException {
    // 1. generate the pdf
    PdfFile filledPdf = pdfGenerator.generate(submission.getFlow(), submission);
    filledPdf.finalizeForSending();
    byte[] pdfByteArray = filledPdf.fileBytes();
    filledPdf.deleteFile();
    return pdfByteArray;
  }

  /**
   * Generates a generic pdf file name from the flow and submission id that are part of the Submission.
   *
   * @param submission Submission to create the PDF filename for
   * @return a generic filename string, including the '.pdf' extension
   */
  public String generatePdfName(Submission submission) {
    return String.format("%s_%s.pdf", submission.getFlow(), submission.getId().toString());
  }
}
