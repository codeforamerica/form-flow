package formflow.library.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import formflow.library.data.Submission;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
class PdfServiceTest {

  @Autowired
  PdfService pdfService;

  @Test
  void generate() throws IOException {
    Map<String, Object> inputData = Map.of(
        "firstName", "Michaelā",
        "city", "San Francisco"
    );
    Submission submission = Submission.builder()
        .flow("ubi")
        .submittedAt((DateTime.parse("2023-11-28").toDate()))
        .inputData(inputData)
        .build();
    File pdfFile = pdfService.generate(submission);
    String text = getText(pdfFile);
    assertThat(text).contains("San Francisco");
    assertThat(text).contains("Michael ā"); // Extra space to account for idiosyncrasies in text extraction from PDFs
    assertThat(text).contains("11/28/2023");
  }

  private static String getText(File file) throws IOException {
    try (PdfReader reader = new PdfReader(file.getPath())) {
      PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(reader);
      return pdfTextExtractor.getTextFromPage(1);
    }
  }
}
