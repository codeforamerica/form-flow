package formflow.library.pdf;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PDFBoxFieldFillerTest {

  private final PDFBoxFieldFiller PDFBoxFieldFiller = new PDFBoxFieldFiller(
      List.of(
          new ClassPathResource("/pdfs/Multipage-UBI-Form.pdf")
      )
  );

  @Test
  void shouldMapTextFields() throws IOException {
    String expectedFieldValue = "Michael";
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", expectedFieldValue)
    );

    ApplicationFile applicationFile = PDFBoxFieldFiller.fill(fields, "");

    PDAcroForm acroForm = getPdAcroForm(applicationFile);

    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedFieldValue);
  }

  /**
   * <p>
   * This test makes sure that if a PDFfield is passed a null value, that the value is set to an empty string when
   * PDFBoxFieldFiller is called.
   * </p>
   **/


  @Test
  void shouldSetTheAppropriateNonValueForTheFieldType() throws IOException {
    Collection<PdfField> fields = List.of(
        new PdfField("TEXT_FIELD", null)
    );

    ApplicationFile applicationFile = PDFBoxFieldFiller.fill(fields, "");

    PDAcroForm acroForm = getPdAcroForm(applicationFile);
    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo("");
  }

//  @Test
//  void shouldMapMultipleChoiceFields() throws IOException {
//    Collection<PdfField> fields = List.of(
//        new BinaryPdfField("BINARY_FIELD_1"),
//        new BinaryPdfField("BINARY_FIELD_3")
//    );
//
//    ApplicationFile applicationFile = PDFBoxFieldFiller.fill(fields, "", "");
//
//    PDAcroForm acroForm = getPdAcroForm(applicationFile);
//    assertThat(acroForm.getField("BINARY_FIELD_1").getValueAsString()).isEqualTo("Yes");
//    assertThat(acroForm.getField("BINARY_FIELD_3").getValueAsString()).isEqualTo("Yes");
//  }

  @Test
  void shouldReturnTheAppropriateFilename() {
    String fileName = "fileName.pdf";
    ApplicationFile applicationFile = PDFBoxFieldFiller.fill(emptyList(), fileName);
    assertThat(applicationFile.fileName()).isEqualTo(fileName);
  }

  @Test
  void shouldConcatenateAllResourcePDFs() throws IOException {
    ApplicationFile applicationFile = PDFBoxFieldFiller.fill(emptyList(), "");

    Path path = Files.createTempDirectory("");
    File file = new File(path.toFile(), "test_file.pdf");
    Files.write(file.toPath(), applicationFile.fileBytes());
    PDDocument pdDocument = PDDocument.load(file);

    assertThat(pdDocument.getNumberOfPages()).isEqualTo(4);
  }

  @Test
  void shouldNotThrowException_whenFieldIsNotFound() {
    assertThatCode(() -> PDFBoxFieldFiller.fill(List.of(
            new PdfField("definitely-not-a-field", "")),
        "test_file.txt")).doesNotThrowAnyException();
  }

//  @Test
//  void shouldSupportEmojis() throws IOException {
//    String submittedValue = "Michael😃";
//    String expectedValue = "Michael😃";
//
//    Collection<PdfField> fields = List.of(
//        new PdfField("TEXT_FIELD", submittedValue)
//    );
//    ApplicationFile applicationFile = PDFBoxFieldFiller.fill(fields, "", "");
//    PDAcroForm acroForm = getPdAcroForm(applicationFile);
//
//    assertThat(acroForm.getField("TEXT_FIELD").getValueAsString()).isEqualTo(expectedValue);
//  }

  private PDAcroForm getPdAcroForm(ApplicationFile applicationFile) throws IOException {
    Path path = Files.createTempDirectory("");
    File file = new File(path.toFile(), "Multipage-UBI-Form.pdf");
    Files.write(file.toPath(), applicationFile.fileBytes());

    PDDocument pdDocument = PDDocument.load(file);
    return pdDocument.getDocumentCatalog().getAcroForm();
  }
}
