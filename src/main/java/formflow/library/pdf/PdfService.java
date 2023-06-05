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
   * @param flow
   * @param submission
   * @return a pdf byte array
   */
  public byte[] getFilledOutPDF(String flow, Submission submission) throws IOException {
    // 1. generate the pdf
    PdfFile filledPdf = pdfGenerator.generate(flow, submission);
    filledPdf.finalizeForSending();
    byte[] pdfByteArray = filledPdf.fileBytes();
    filledPdf.deleteFile();
    return pdfByteArray;
  }

  /**
   * Generates a generic pdf file name from a flow and submissionId
   *
   * @param flow
   * @param submission
   * @return a generic filename string
   */
  public String generatePdfName(String flow, Submission submission) {
    return String.format("%s_%s", flow, submission.getId().toString());
  }
}
