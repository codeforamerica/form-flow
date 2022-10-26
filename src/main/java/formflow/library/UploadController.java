package formflow.library;

import formflow.library.data.UploadedFileRepositoryService;
import formflow.library.upload.CloudFileRepository;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@Controller
@EnableAutoConfiguration
@Slf4j
public class UploadController {

  private final UploadedFileRepositoryService uploadedFileRepositoryService;

  private final CloudFileRepository cloudFileRepository;

  public UploadController(
      UploadedFileRepositoryService uploadedFileRepositoryService,
      CloudFileRepository cloudFileRepository
  ) {
    this.uploadedFileRepositoryService = uploadedFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
  }

  @PostMapping("/file-upload")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("type") String type,
      HttpSession httpSession
  ) throws IOException, InterruptedException {
    try {
      log.info("You are in file upload endpoint");
      log.info("The file name is " + file.getOriginalFilename());
      Long id = (Long) httpSession.getAttribute("id");
      UUID uuid = UUID.randomUUID();
      String s3FilePath = String.format("%s/%s", httpSession.getAttribute("id"), uuid);
      cloudFileRepository.upload(s3FilePath, file);
//      submissionRepositoryService.findById(id);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      log.error("Error Occurred while uploading File " + e.getLocalizedMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
