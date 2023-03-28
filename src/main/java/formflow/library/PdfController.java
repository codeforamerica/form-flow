package formflow.library;

import formflow.library.data.SubmissionRepositoryService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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

  PdfController(SubmissionRepositoryService submissionRepositoryService) {
    super(submissionRepositoryService);
  }

  @GetMapping("{flow}/{submissionId}")
  ResponseEntity<File> downloadPdf(
      @PathVariable String flow,
      @PathVariable String submissionId,
      HttpSession httpSession
  ) throws IOException {
    log.info("Printing....PDF with submission_id: " + submissionId);
    File outputFile = File.createTempFile("java", "pdf");
    byte[] bytesFromFile = Files.readAllBytes(Path.of("src/main/resources/pdfs/Page-1-UBI-form.pdf"));
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
      outputStream.write(bytesFromFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(outputFile);
  }
}
