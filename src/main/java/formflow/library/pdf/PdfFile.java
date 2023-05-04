package formflow.library.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.system.ApplicationTemp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public record PdfFile(PDDocument pdDocument, String fileName) {
  public PdfFile(String filePath, String fileName) throws IOException {
    this(PDDocument.load(PdfFile.class.getResourceAsStream(filePath)), createTmpFile(filePath));
  }

  private static String createTmpFile(String filePath) throws IOException {
    InputStream blankPdf = PdfFile.class.getResourceAsStream(filePath);
    int fileExtension = filePath.length() - 3;
    File tempFile = File.createTempFile(filePath, String.valueOf(fileExtension), new ApplicationTemp().getDir());
    Path pdfToBeWritten = Files.write(tempFile.toPath(), Objects.requireNonNull(blankPdf).readAllBytes());
    return pdfToBeWritten.toAbsolutePath().toString();
  }

  @Override
  public String toString() {
    return fileName;
  }

  public byte[] fileBytes() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    pdDocument.save(outputStream);
    return outputStream.toByteArray();
  }
}
