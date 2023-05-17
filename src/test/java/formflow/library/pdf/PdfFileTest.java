package formflow.library.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Pdf;
import org.springframework.boot.system.ApplicationTemp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFileTest {

  PdfFile tempPdfFile;
  String testPdfPath = "/pdfs/testPdf.pdf";

  @BeforeEach
  void setUp() {
    tempPdfFile = PdfFile.copyToTempFile(testPdfPath);
  }

  @AfterEach
  void tearDown() throws IOException {
    tempPdfFile.deleteFile();
  }

  @Test
  void copyToTempFileCopiesFileToTempDirectory() throws IOException {
    InputStream unfilledTestPdf = PdfFile.class.getResourceAsStream(testPdfPath);

    assertThat(tempPdfFile.path()).contains(new ApplicationTemp().getDir().getAbsolutePath(), "testPdf", "pdf");
    assertThat(tempPdfFile.fileBytes()).isEqualTo(unfilledTestPdf.readAllBytes());
  }

  @Test
  void finalizeForSendingRemovesFieldsToLockEditingAndPreserveViewCompatibility() throws IOException {
    byte[] originalBytes = tempPdfFile.fileBytes();
    tempPdfFile.finalizeForSending();

    assertThat(PDDocument.load(tempPdfFile.fileBytes()).getDocumentCatalog().getAcroForm().getFields()).isEmpty();
    assertThat(originalBytes.length).isGreaterThan(tempPdfFile.fileBytes().length);
  }

  @Test
  void copyToTempFilesMultipleFiles() throws IOException {
    PdfFile onePdfFile = PdfFile.copyToTempFile(testPdfPath);
    PdfFile twoPdfFile = PdfFile.copyToTempFile(testPdfPath);
    PdfFile threePdfFile = PdfFile.copyToTempFile(testPdfPath);
    PdfFile fourPdfFile = PdfFile.copyToTempFile(testPdfPath);

    // assert that names via PdfFile are the same
    assertThat(onePdfFile.name()).isEqualTo(twoPdfFile.name());
    assertThat(onePdfFile.name()).isEqualTo(threePdfFile.name());
    assertThat(onePdfFile.name()).isEqualTo(fourPdfFile.name());

    // there is a random set of characters at end of path, so these should not match
    assertThat(onePdfFile.path()).isNotEqualTo(twoPdfFile.path());
    assertThat(onePdfFile.path()).isNotEqualTo(threePdfFile.path());
    assertThat(onePdfFile.path()).isNotEqualTo(fourPdfFile.path());

    String oneFileName = Path.of(onePdfFile.path()).getFileName().toString();
    String twoFileName = Path.of(twoPdfFile.path()).getFileName().toString();
    String threeFileName = Path.of(threePdfFile.path()).getFileName().toString();
    String fourFileName = Path.of(fourPdfFile.path()).getFileName().toString();

    // names via the Path are not the same
    assertThat(oneFileName).isNotEqualTo(twoFileName);
    assertThat(oneFileName).isNotEqualTo(threeFileName);
    assertThat(oneFileName).isNotEqualTo(fourFileName);

    // but the path's w/o the full filename (with random chars) should match
    String pathOne = onePdfFile.path().replace(oneFileName, "");
    String pathTwo = twoPdfFile.path().replace(twoFileName, "");
    String pathThree = threePdfFile.path().replace(threeFileName, "");
    String pathFour = fourPdfFile.path().replace(fourFileName, "");

    assertThat(pathOne).isEqualTo(pathTwo);
    assertThat(pathOne).isEqualTo(pathThree);
    assertThat(pathOne).isEqualTo(pathFour);

    // cleanup
    onePdfFile.deleteFile();
    twoPdfFile.deleteFile();
    threePdfFile.deleteFile();
    fourPdfFile.deleteFile();
  }

  @Test
  void createPdfFilesAndEnsureDeletion() throws IOException {
    PdfFile onePdfFile = PdfFile.copyToTempFile(testPdfPath);
    PdfFile twoPdfFile = PdfFile.copyToTempFile(testPdfPath);
    PdfFile threePdfFile = PdfFile.copyToTempFile(testPdfPath);

    onePdfFile.deleteFile();
    twoPdfFile.deleteFile();
    threePdfFile.deleteFile();

    assertThat(Files.exists(Path.of(onePdfFile.path()))).isFalse();
    assertThat(Files.exists(Path.of(twoPdfFile.path()))).isFalse();
    assertThat(Files.exists(Path.of(threePdfFile.path()))).isFalse();
  }
}