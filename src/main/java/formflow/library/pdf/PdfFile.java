package formflow.library.pdf;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.system.ApplicationTemp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public record PdfFile(String path, String name) {

  // put all the file in the same tmp java location
  private static final File tmpFileDir = new ApplicationTemp().getDir();

  public static PdfFile copyToTempFile(String pathToResource) {
    InputStream unfilledPdf = PdfFile.class.getResourceAsStream(pathToResource);
    Path resourcePath = Path.of(pathToResource);
    String fileNameWithExtension = resourcePath.getFileName().toString();
    String fileExtension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf('.'));
    String fileName = fileNameWithExtension.replaceAll(fileExtension + "$", "");

    File tempFile;
    Path pathToTempFile;
    try {
      tempFile = File.createTempFile(fileName, fileExtension, tmpFileDir);
      pathToTempFile = Files.write(tempFile.toPath(), unfilledPdf.readAllBytes());
    } catch (NullPointerException | IOException e) {
      throw new RuntimeException("Could not copy pdf resource to temp file: " + e);
    }
    return new PdfFile(pathToTempFile.toAbsolutePath().toString(), fileName);
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

  public void deleteFile() throws IOException {
    Files.delete(Path.of(this.path));
  }
}
