package formflow.library.pdf;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PDFFormFillerTest {

  private final PDFFormFiller pdfFormFiller = new PDFFormFiller();
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
  @Disabled
  void shouldSetNullTextFieldsAsEmptyString() throws IOException {
    Collection<PdfField> fields = List.of(
            new PdfField("TEXT_FIELD", null)
    );
    File file = pdfFormFiller.fill(pdf, fields);

    Assertions.assertEquals(getField(file, "TEXT_FIELD"), "");

  }

  @Test
  void shouldAccept_a_with_macron() throws IOException {
    String textFieldValue = "北京";
    Collection<PdfField> fields = List.of(
            new PdfField("TEXT_FIELD", textFieldValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, false);

    assertEquals(textFieldValue, getField(file, "TEXT_FIELD"));
  }

  @Test
  void shouldAcceptSimplifiedChinese() throws IOException {
    String textFieldValue = "北京";
    Collection<PdfField> fields = List.of(
            new PdfField("TEXT_FIELD", textFieldValue)
    );

    File file = pdfFormFiller.fill(pdf, fields, false);

    assertEquals(textFieldValue, getField(file, "TEXT_FIELD"));
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
}
