package formflow.library.pdf;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

public class PdfTest {

  private PDAcroForm pdf;

  protected void assertPdfFieldEquals(String fieldName, String expectedValue) {
    PDField field = pdf.getField(fieldName);
    assertThat(field).isNotNull();
    String pdfFieldText = field.getValueAsString();
    assertThat(pdfFieldText).isEqualTo(expectedValue);
  }

  protected byte[] getBytesFromTestPdf(String path) throws IOException {
    return requireNonNull(getClass().getClassLoader().getResourceAsStream("pdfs/" + path)).readAllBytes();
  }

  protected void preparePdfForAssertions(ApplicationFile filledPdf) throws IOException {
    pdf = PDDocument.load(filledPdf.fileBytes()).getDocumentCatalog().getAcroForm();
  }
}
