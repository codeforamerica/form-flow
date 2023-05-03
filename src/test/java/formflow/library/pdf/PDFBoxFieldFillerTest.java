package formflow.library.pdf;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PDFBoxFieldFillerTest {

  private PDFBoxFieldFiller pdfBoxFieldFiller;

  @BeforeEach
  void setUp() {
    pdfBoxFieldFiller = new PDFBoxFieldFiller(List.of(
        new ClassPathResource("/pdfs/testPdf.pdf"),
        new ClassPathResource("/pdfs/blankPdf.pdf")
    ));
  }

  @Test
  void shouldMapTextFields() throws IOException {
    String expectedFieldValue = "Michael";
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", expectedFieldValue)
    );

    PDDocument pdfFile = pdfBoxFieldFiller.fill(fields, "");
    PDAcroForm acroForm = pdfFile.getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedFieldValue);
  }

  @Test
  void shouldSetNullTextFieldsAsEmptyString() throws IOException {
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", null)
    );
    PDDocument pdfFile = pdfBoxFieldFiller.fill(fields, "");

    PDAcroForm acroForm = pdfFile.getDocumentCatalog().getAcroForm();
    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo("");
  }

  @Test
  void shouldConcatenateAllResourcePDFs() throws IOException {
    assertThat(pdfBoxFieldFiller.fill(emptyList(), "").getNumberOfPages()).isEqualTo(2);
  }

  @Test
  void shouldNotThrowException_whenFieldIsNotFound() {
    assertThatCode(() -> pdfBoxFieldFiller.fill(List.of(
            new PdfField("definitely-not-a-field", "")),
        "test_file.txt")).doesNotThrowAnyException();
  }

  @Test
  void shouldSupportEmojis() {
    String submittedValue = "Michael😃";
    String expectedValue = "Michael😃";

    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", submittedValue)
    );
    PDAcroForm acroForm = pdfBoxFieldFiller.fill(fields, "").getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedValue);
  }

}
