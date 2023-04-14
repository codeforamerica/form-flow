package formflow.library;

import static java.util.Objects.requireNonNull;

import formflow.library.pdf.ApplicationFile;
import formflow.library.pdf.PdfGenerator;
import formflow.library.pdf.PdfMapConfiguration;
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
  private final PdfMapConfiguration pdfMapConfiguration;
//  private final List<PdfMapConfiguration> pdfMapConfigurations;

  public PdfController(PdfGenerator pdfGenerator, PdfMapConfiguration pdfMapConfiguration) {
    this.pdfGenerator = pdfGenerator;
    this.pdfMapConfiguration = pdfMapConfiguration;
//    this.pdfMapConfigurations = pdfMapConfigurations;
  }

  @GetMapping("{flow}/{submissionId}")
  ResponseEntity<byte[]> downloadPdf(
      @PathVariable String flow,
      @PathVariable String submissionId,
      HttpSession httpSession
  ) throws IOException {
    log.info("Downloading PDF with submission_id: " + submissionId);
    ApplicationFile filledPdf = pdfGenerator.generate(pdfMapConfiguration.getPdfFromFlow(flow), UUID.fromString(submissionId));
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%s-%s.pdf".formatted(filledPdf.fileName(), submissionId));
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).headers(headers).body(filledPdf.fileBytes());
  }
}
