package formflow.library;

import formflow.library.data.SubmissionRepositoryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class PdfController extends FormFlowController {

  @Value("${form-flow.pdf.path}")
  String configPath;

  PdfController(SubmissionRepositoryService submissionRepositoryService) {
    super(submissionRepositoryService);
  }

  @GetMapping("{flow}/{submissionId}/{nameOfDocument}")
  ResponseEntity<byte[]> downloadPdf(
      @PathVariable String flow,
      @PathVariable String submissionId,
      @PathVariable String nameOfDocument,
      HttpSession httpSession
  ) throws IOException {
    log.info("Downloading PDF with submission_id: " + submissionId);
    byte[] bytesFromFile = Files.readAllBytes(Path.of(configPath + "Multipage-UBI-Form.pdf"));
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nameOfDocument + "-" + submissionId + ".pdf");
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).headers(headers).body(bytesFromFile);
  }
}
