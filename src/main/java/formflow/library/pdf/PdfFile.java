package formflow.library.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public record PdfFile(PDDocument pdDocument, String fileName) {

  public PdfFile(String filePath, String fileName) throws IOException {
    this(PDDocument.load(PdfFile.class.getResourceAsStream(filePath)), fileName);
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
