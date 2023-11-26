package formflow.library.pdf;

import formflow.library.data.Submission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
    File file = pdfGenerator.generate(submission.getFlow(), submission);
    byte[] pdfByteArray = Files.readAllBytes(file.toPath());
    file.delete();
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
