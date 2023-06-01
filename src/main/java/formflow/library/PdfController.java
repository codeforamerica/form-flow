package formflow.library;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.pdf.PdfFile;
import formflow.library.pdf.PdfGenerator;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class PdfController extends FormFlowController {

  private final MessageSource messageSource;
  private final PdfGenerator pdfGenerator;

  public PdfController(MessageSource messageSource, PdfGenerator pdfGenerator,
      SubmissionRepositoryService submissionRepositoryService) {
    super(submissionRepositoryService);
    this.messageSource = messageSource;
    this.pdfGenerator = pdfGenerator;
  }

  @GetMapping("{flow}/{submissionId}")
  ResponseEntity<?> downloadPdf(
      @PathVariable String flow,
      @PathVariable String submissionId,
      HttpSession httpSession
  ) throws IOException {
    Optional<Submission> maybeSubmission = submissionRepositoryService.findById(UUID.fromString(submissionId));
    if (httpSession.getAttribute("id").toString().equals(submissionId) && maybeSubmission.isPresent()) {
      log.info("Downloading PDF with submission_id: " + submissionId);
      Submission submission = maybeSubmission.get();
      HttpHeaders headers = new HttpHeaders();
      PdfFile filledPdf = pdfGenerator.generate(flow, submission);

      filledPdf.finalizeForSending();
      byte[] data = filledPdf.fileBytes();
      filledPdf.deleteFile();

      headers.add(HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=%s-%s.pdf".formatted(filledPdf.name(), submissionId));
      return ResponseEntity
          .ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .headers(headers)
          .body(data);
    } else {
      log.error(
          "Attempted to download PDF with submission_id: " + submissionId + " but session_id was: "
              + httpSession.getAttribute(
              "id"));
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.forbidden", null, null));
    }
  }
}
