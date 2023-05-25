package formflow.library;

import formflow.library.pdf.PdfFile;
import formflow.library.pdf.PdfGenerator;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <b>PdfService</b> is a service that generates a byte[] of a flattened pdf.
 */

@Service
@Slf4j
public class PdfService {

  private final PdfGenerator pdfGenerator;

  public PdfService(PdfGenerator pdfGenerator) {
    this.pdfGenerator = pdfGenerator;
  }

  //returns a filled out pdf file

  /**
   * <p>Uses pdfGenerator to generate a pdf for a flow using a UUID.</p>
   *
   * @param flow
   * @param submissionId
   * @return a pdf byte array
   */
  public byte[] getFilledOutPDF(String flow, String submissionId) throws IOException {
    // 1. generate the pdf
    PdfFile filledPdf = pdfGenerator.generate(flow, UUID.fromString(submissionId));
    filledPdf.finalizeForSending();
    byte[] pdfByteArray = filledPdf.fileBytes();
    filledPdf.deleteFile();
    return pdfByteArray;
  }

  /**
   * Generates a generic pdf file name from a flow and submissionId
   *
   * @param flow
   * @param submissionId
   * @return a generic filename string
   */
  public String generatePdfName(String flow, String submissionId) {
    return String.format("%s_%s.pdf", submissionId, flow);
  }

}
