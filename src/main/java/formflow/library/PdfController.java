package formflow.library;

import formflow.library.pdf.ApplicationFile;
import formflow.library.pdf.PdfGenerator;
import formflow.library.pdf.PdfLocationConfiguration;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@EnableAutoConfiguration
@Slf4j
@RequestMapping("/download")
public class PdfController {


  @Value("${form-flow.pdf.path}")
  private String configPath;

  private final PdfGenerator pdfGenerator;
  private final PdfLocationConfiguration pdfLocationConfiguration;

  public PdfController(PdfGenerator pdfGenerator, PdfLocationConfiguration pdfLocationConfiguration) {
    this.pdfGenerator = pdfGenerator;
    this.pdfLocationConfiguration = pdfLocationConfiguration;
  }

  @GetMapping("{flow}/{submissionId}/{nameOfDocument}")
  ResponseEntity<byte[]> downloadPdf(
      @PathVariable String flow,
      @PathVariable String submissionId,
      @PathVariable String nameOfDocument,
      HttpSession httpSession
  ) throws IOException {
    log.info("Downloading PDF with submission_id: " + submissionId);
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nameOfDocument + "-" + submissionId + ".pdf");
    ApplicationFile filledPdf = pdfGenerator.generate(pdfLocationConfiguration.get(nameOfDocument), UUID.fromString(submissionId));
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).headers(headers).body(filledPdf.fileBytes());
  }
}
