package formflow.library.pdf;

import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfTest {
  private PDAcroForm pdf;

  protected void assertPdfFieldEquals(String fieldName, String expectedValue) {
    PDField field = pdf.getField(fieldName);
    assertThat(field).isNotNull();
    String pdfFieldText = field.getValueAsString();
    assertThat(pdfFieldText).isEqualTo(expectedValue);
  }

  protected void preparePdfForAssertions(PdfFile filledPdf) {
    pdf = filledPdf.pdDocument().getDocumentCatalog().getAcroForm();
  }
}
