package formflow.library.pdf;

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

  private static final File tmpFileDir = new ApplicationTemp().getDir();

  /**
   * This static function will take in a path to a template file and create a PdfFile object to return to the caller. It will
   * create temp space on the filesystem that the PDF file is read into.
   *
   * @param pathToResource String path to PDF resource to read in
   * @return PdfFile object representing the PDF indicated.
   */
  public static PdfFile copyToTempFile(String pathToResource) {
    InputStream unfilledPdf = PdfFile.class.getResourceAsStream(pathToResource);
    String fileNameWithExtension = Path.of(pathToResource).getFileName().toString();
    String fileExtension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf('.'));
    String fileName = fileNameWithExtension.replaceAll(fileExtension + "$", "");

    File tempFile;
    Path pathToTempFile;
    try {
      tempFile = File.createTempFile(fileName, fileExtension, tmpFileDir);
      pathToTempFile = Files.write(tempFile.toPath(), unfilledPdf.readAllBytes());
    } catch (NullPointerException | IOException e) {
      throw new RuntimeException(
          String.format(
              "Could not copy pdf resource file (%s) to the temp file location (%s): %s",
              pathToResource,
              tmpFileDir.toString(),
              e)
      );
    }
    return new PdfFile(pathToTempFile.toAbsolutePath().toString(), fileName);
  }

  @Override
  public String toString() {
    return path;
  }

  /**
   * Returns a byte array containing all the contents of the PDF file.
   *
   * @return Byte array of file contents
   * @throws IOException Thrown if the file cannot be read in.
   */
  public byte[] fileBytes() throws IOException {
    return Files.readAllBytes(Paths.get(this.path));
  }

  /**
   * Does all the tasks necessary to finalize the PDF file. It will flatten the PDF file.
   *
   * @throws IOException Thrown if the file cannot be read in.
   */
  public void finalizeForSending() throws IOException {
    PDDocument pdDocument = PDDocument.load(fileBytes());
    pdDocument.getDocumentCatalog().getAcroForm().flatten();
    pdDocument.save(path);
    pdDocument.close();
  }

  /**
   * Deletes the underlying temporary file from the file system.
   *
   * @throws IOException Thrown if the file cannot be worked with.
   */
  public void deleteFile() throws IOException {
    log.info(String.format("Deleting temporary file: %s", this.path));
    Files.delete(Path.of(this.path));
  }
}
