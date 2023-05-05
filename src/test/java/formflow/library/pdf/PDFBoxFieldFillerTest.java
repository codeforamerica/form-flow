package formflow.library.pdf;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import java.util.Collection;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PDFBoxFieldFillerTest {

  private PDFBoxFieldFiller pdfBoxFieldFiller = new PDFBoxFieldFiller();

  @Test
  void shouldMapAllFieldTypes() {
    String expectedFieldValue = "Michael";
    String radioValue = "option2";
    String checkboxSelectedValue = "Yes";
    String unselectedCheckboxValue = "Off";

    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", expectedFieldValue),
        new PdfField("RADIO_BUTTON", radioValue),
        new PdfField("CHECKBOX_OPTION_1", checkboxSelectedValue),
        new PdfField("CHECKBOX_OPTION_3", checkboxSelectedValue)
    );

    PDDocument pdfFile = pdfBoxFieldFiller.fill(List.of(
        new ClassPathResource("/pdfs/testPdf.pdf"),
        new ClassPathResource("/pdfs/blankPdf.pdf")
    ), fields, "");
    PDAcroForm acroForm = pdfFile.getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedFieldValue);
    assertThat(acroForm.getField("RADIO_BUTTON").getValueAsString()).isEqualTo(radioValue);
    assertThat(acroForm.getField("CHECKBOX_OPTION_1").getValueAsString()).isEqualTo(checkboxSelectedValue);
    assertThat(acroForm.getField("CHECKBOX_OPTION_2").getValueAsString()).isEqualTo(unselectedCheckboxValue);
    assertThat(acroForm.getField("CHECKBOX_OPTION_3").getValueAsString()).isEqualTo(checkboxSelectedValue);
  }

  @Test
  void shouldSetNullTextFieldsAsEmptyString() {
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", null)
    );
    PDDocument pdfFile = pdfBoxFieldFiller.fill(List.of(
        new ClassPathResource("/pdfs/testPdf.pdf"),
        new ClassPathResource("/pdfs/blankPdf.pdf")
    ), fields, "");

    PDAcroForm acroForm = pdfFile.getDocumentCatalog().getAcroForm();
    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo("");
  }

  @Test
  void shouldConcatenateAllResourcePDFs() {
    assertThat(pdfBoxFieldFiller.fill(List.of(
        new ClassPathResource("/pdfs/testPdf.pdf"),
        new ClassPathResource("/pdfs/blankPdf.pdf")
    ), emptyList(), "").getNumberOfPages()).isEqualTo(2);
  }

  @Test
  void shouldNotThrowException_whenFieldIsNotFound() {
    assertThatCode(() -> pdfBoxFieldFiller.fill(List.of(
            new ClassPathResource("/pdfs/testPdf.pdf"),
            new ClassPathResource("/pdfs/blankPdf.pdf")
        ),
        List.of(new PdfField("definitely-not-a-field", "")),
        "test_file.txt")
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldSupportEmojis() {
    String submittedValue = "Michael😃";
    String expectedValue = "Michael😃";

    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", submittedValue)
    );
    PDAcroForm acroForm = pdfBoxFieldFiller.fill(List.of(
        new ClassPathResource("/pdfs/testPdf.pdf"),
        new ClassPathResource("/pdfs/blankPdf.pdf")
    ), fields, "").getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedValue);
  }
}
