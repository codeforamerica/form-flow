package formflow.library;

import formflow.library.pdf.PdfFile;
import formflow.library.pdf.PdfGenerator;
import jakarta.servlet.http.HttpSession;
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

import java.io.IOException;
import java.util.UUID;

@Controller
@EnableAutoConfiguration
@Slf4j
@RequestMapping("/download")
public class PdfController {


  @Value("${form-flow.pdf.path}")
  private String configPath;

  private final PdfGenerator pdfGenerator;

  public PdfController(PdfGenerator pdfGenerator) {
    this.pdfGenerator = pdfGenerator;
  }

  @GetMapping("{flow}/{submissionId}")
  ResponseEntity<byte[]> downloadPdf(
      @PathVariable String flow,
      @PathVariable String submissionId,
      HttpSession httpSession
  ) throws IOException {
    log.info("Downloading PDF with submission_id: " + submissionId);
    PdfFile filledPdf = pdfGenerator.generate(flow, UUID.fromString(submissionId));
    filledPdf.finalizeForSending();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%s-%s.pdf".formatted(filledPdf.path(), submissionId));
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).headers(headers).body(filledPdf.fileBytes());
  }
}
