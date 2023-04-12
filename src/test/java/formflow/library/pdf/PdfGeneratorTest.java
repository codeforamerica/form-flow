package formflow.library.pdf;

import formflow.library.data.Submission;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfGeneratorTest extends PdfTest {

  private PdfGenerator pdfGenerator;
  private Submission submission;
  private String testPdfName;


  @BeforeEach
  void setUp() {
    pdfGenerator = new PdfGenerator();
    submission = Submission.builder().id(UUID.randomUUID()).inputData(
        Map.of("firstName", "Greatest", "lastName", "Ever")).build();
    testPdfName = "Multipage-UBI-Form.pdf";
  }

  @Test
  void generateReturnsAFilledPdf() throws IOException {
    preparePdfForAssertions(
        pdfGenerator.generate(new ApplicationFile(getBytesFromTestPdf(testPdfName), testPdfName), submission.getId())
    );

    assertPdfFieldEquals("FIRST_NAME", submission.getInputData().get("firstName").toString());
    assertPdfFieldEquals("LAST_NAME", submission.getInputData().get("lastName").toString());
  }

}