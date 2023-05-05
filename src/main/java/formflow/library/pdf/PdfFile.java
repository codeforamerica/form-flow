package formflow.library.pdf;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.system.ApplicationTemp;

@Slf4j
public record PdfFile(String path, String name) {

  public static PdfFile copyToTempFile(PdfFile file) {
    InputStream blankPdf = PdfFile.class.getResourceAsStream(file.path());
    int fileExtension = file.path.length() - 3;
    File tempFile = null;
    Path pathToTempFile;
    try {
      tempFile = File.createTempFile(file.name(), String.valueOf(fileExtension), new ApplicationTemp().getDir());
      pathToTempFile = Files.write(tempFile.toPath(), requireNonNull(blankPdf).readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException("Could not copy pdf resource to temp file: " + e);
    }
    return new PdfFile(pathToTempFile.toAbsolutePath().toString(), file.name());
  }

  @Override
  public String toString() {
    return path;
  }

  public byte[] fileBytes() throws IOException {
    return requireNonNull(PdfFile.class.getResourceAsStream(this.path)).readAllBytes();
  }

  public PDDocument loadPdDocument() throws IOException {
    return PDDocument.load(PdfFile.class.getResourceAsStream(this.path));
  }

  public void finalizeForSending() throws IOException {
    PDDocument pdDocument = loadPdDocument();
    pdDocument.getDocumentCatalog().getAcroForm().flatten();
    pdDocument.close();
  }
}
