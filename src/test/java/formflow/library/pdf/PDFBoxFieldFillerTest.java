package formflow.library.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class PDFBoxFieldFillerTest {

  private final PDFBoxFieldFiller pdfBoxFieldFiller = new PDFBoxFieldFiller();
  private final String pdf = "/pdfs/testPdf.pdf";

  @Test
  void shouldMapAllFieldTypes() throws IOException {
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

    PdfFile pdfFile = pdfBoxFieldFiller.fill(pdf, fields);
    PDDocument pdDocument = Loader.loadPDF(pdfFile.fileBytes());
    PDAcroForm acroForm = pdDocument.getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedFieldValue);
    assertThat(acroForm.getField("RADIO_BUTTON").getValueAsString()).isEqualTo(radioValue);
    assertThat(acroForm.getField("CHECKBOX_OPTION_1").getValueAsString()).isEqualTo(checkboxSelectedValue);
    assertThat(acroForm.getField("CHECKBOX_OPTION_2").getValueAsString()).isEqualTo(unselectedCheckboxValue);
    assertThat(acroForm.getField("CHECKBOX_OPTION_3").getValueAsString()).isEqualTo(checkboxSelectedValue);

    pdDocument.close();
  }
  @Test
  void shouldSubstituteUnsupportedCharactersForQuestionsWhenFillingPdfFields() throws IOException {
    String unsupportedValue = "テスト";
    String unsupportedPdfField = "APPLICANT_LEGAL_NAME_FIRST";
    String unsupportedReturnValue = "???";
    String supportedValue = "Jones";
    String supportedPdfField = "TEXT_FIELD";
    PdfField unsupportedPdfFieldObject = new PdfField(supportedPdfField, supportedValue);

    Collection<PdfField> fields = List.of(
        unsupportedPdfFieldObject,
        new PdfField(unsupportedPdfField, unsupportedValue)
    );
    String ubiPdf =  "/pdfs/Multipage-UBI-Form.pdf";
    PdfFile pdfFile = pdfBoxFieldFiller.fill(ubiPdf, fields);

    PDDocument pdDocument = Loader.loadPDF(pdfFile.fileBytes());
    PDAcroForm acroForm = pdDocument.getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField(supportedPdfField).getValueAsString()).isEqualTo(supportedValue);
    assertThat(acroForm.getField(unsupportedPdfField).getValueAsString()).isEqualTo(unsupportedReturnValue);
    pdDocument.close();

  }
  @Test
  void shouldSetNullTextFieldsAsEmptyString() throws IOException {
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", null)
    );
    PdfFile pdfFile = pdfBoxFieldFiller.fill(pdf, fields);

    PDDocument pdDocument = Loader.loadPDF(pdfFile.fileBytes());
    assertThat(pdDocument.getDocumentCatalog().getAcroForm().getField("TEXT_FIELD").getValueAsString()).isEqualTo("");
    pdDocument.close();
  }

  @Test
  void shouldNotThrowException_whenFieldIsNotFound() {
    assertThatCode(() -> pdfBoxFieldFiller.fill(pdf,
            List.of(new PdfField("definitely-not-a-field", ""))
        )
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldSupportEmojis() throws IOException {
    String submittedValue = "Michael😃";
    String expectedValue = "Michael😃";

    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", submittedValue)
    );

    PdfFile pdfFile = pdfBoxFieldFiller.fill(pdf, fields);
    PDDocument pdDocument = Loader.loadPDF(pdfFile.fileBytes());
    PDAcroForm acroForm = pdDocument.getDocumentCatalog().getAcroForm();

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedValue);
    pdDocument.close();
  }
}
