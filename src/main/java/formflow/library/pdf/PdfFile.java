package formflow.library.pdf;

import static java.nio.file.StandardOpenOption.WRITE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.system.ApplicationTemp;

@Slf4j
public record PdfFile(String path, String name) {

  public static PdfFile copyToTempFile(PdfFile file) {
    InputStream unfilledPdf = PdfFile.class.getResourceAsStream(file.path());
    int fileExtension = file.name.length() - 4;
    File tempFile;
    Path pathToTempFile;
    try {
      tempFile = File.createTempFile(file.name(), file.name.substring(fileExtension), new ApplicationTemp().getDir());
      pathToTempFile = Files.write(tempFile.toPath(), unfilledPdf.readAllBytes(), WRITE);
    } catch (NullPointerException | IOException e) {
      throw new RuntimeException("Could not copy pdf resource to temp file: " + e);
    }
    return new PdfFile(pathToTempFile.toAbsolutePath().toString(), file.name());
  }

  @Override
  public String toString() {
    return path;
  }

  public byte[] fileBytes() throws IOException {
    return Files.readAllBytes(Paths.get(this.path));
  }

  public void finalizeForSending() throws IOException {
    PDDocument pdDocument = PDDocument.load(fileBytes());
    pdDocument.getDocumentCatalog().getAcroForm().flatten();
    pdDocument.save(path);
    pdDocument.close();
  }
}
