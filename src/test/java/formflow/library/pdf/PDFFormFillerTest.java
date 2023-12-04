package formflow.library.pdf;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "form-flow.path=flows-config/test-flow.yaml"})
class PDFFormFillerTest {

  @Autowired
  PDFFormFiller pdfFormFiller;
  private final String pdf = "/pdfs/testPdf.pdf";

  @Test
  void shouldMapAllFieldTypes() throws IOException {
    String textFieldValue = "Michael";
    String radioValue = "option2";
    String checkboxSelectedValue = "Yes";
    String unselectedCheckboxValue = "Off";

    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", textFieldValue),
        new PdfField("RADIO_BUTTON", radioValue),
        new PdfField("CHECKBOX_OPTION_1", checkboxSelectedValue),
        new PdfField("CHECKBOX_OPTION_3", checkboxSelectedValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, false);

    assertEquals(getField(file, "TEXT_FIELD"), textFieldValue);
    assertEquals(getField(file, "RADIO_BUTTON"), radioValue);
    assertEquals(getField(file, "CHECKBOX_OPTION_1"), checkboxSelectedValue);
    assertEquals(getField(file, "CHECKBOX_OPTION_2"), unselectedCheckboxValue);
    assertEquals(getField(file, "CHECKBOX_OPTION_3"), checkboxSelectedValue);
  }

  @Test
  void shouldAccept_a_with_macron() throws IOException {
    String textFieldValue = "Michaelā";
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", textFieldValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, true);

    assertTrue(getText(file).contains("Michael ā")); // openpdf's text extraction introduces a space
  }

  @Test
  void shouldAcceptOtherDiacritic() throws IOException {
    String textFieldValue = "áàäéèêësubstringíîïóôöúûüç";
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", textFieldValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, true);

    assertTrue(getText(file).contains("áàäéèêësubstringíîïóôöúûüç"));
  }

  @Test
  void shouldAcceptSimplifiedChinese() throws IOException {
    String textFieldValue = "北京";
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", textFieldValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, true);

    assertTrue(getText(file).contains(textFieldValue));
  }

  @Test
  void shouldNotThrowException_whenFieldIsNotFound() {
    assertThatCode(() -> pdfFormFiller.fill(pdf,
            List.of(new PdfField("definitely-not-a-field", ""))
        )
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldSupportEmojis() throws IOException {
    String submittedValue = "Michael😃";

    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", submittedValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, false);
    assertEquals(getField(file, "TEXT_FIELD"), submittedValue);
  }

  private static String getField(File file, String fieldName) throws IOException {
    try (PdfReader reader = new PdfReader(file.getPath())) {
      AcroFields form = reader.getAcroFields();
      return form.getField(fieldName);
    }
  }

  private static String getText(File file) throws IOException {
    try (PdfReader reader = new PdfReader(file.getPath())) {
      PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(reader);
      return pdfTextExtractor.getTextFromPage(1);
    }
  }
}
