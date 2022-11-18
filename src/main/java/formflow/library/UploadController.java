package formflow.library;

import com.google.common.io.Files;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UploadedFileRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.upload.CloudFileRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class UploadController extends FormFlowController {

  private final UploadedFileRepositoryService uploadedFileRepositoryService;
  private final CloudFileRepository cloudFileRepository;
  private final ValidationService validationService;

  private final String USER_FILE_ID_KEY = "userFileIds";

  public UploadController(
      UploadedFileRepositoryService uploadedFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      SubmissionRepositoryService submissionRepositoryService, ValidationService validationService) {
    super(submissionRepositoryService);
    this.uploadedFileRepositoryService = uploadedFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
    this.validationService = validationService;
  }

  @PostMapping("/file-upload")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) Map<String, String> formData,
      @RequestParam("flow") String flow,
      @RequestParam(name = "subflow", required = false) String subflow,
      HttpSession httpSession
  ) {
    try {
      // TODO This is not validating anything right now other than that you included the inputName in the corresponding inputs file
      HashMap<String, List<String>> errorMessages = validationService.validate(flow, formData.get("inputName"), file);
      String thumbDataUrl = formData.get("thumbDataURL");
      Submission submission = submissionRepositoryService.findOrCreate(httpSession);
      UUID userFileId = UUID.randomUUID();
      if (submission.getId() == null) {
        submission.setFlow(flow);
        saveToRepository(submission);
        httpSession.setAttribute("id", submission.getId());
      }
      String dropZoneInstanceName = formData.get("inputName");
      String fileExtension = Files.getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
      String uploadLocation = String.format("%s/%s_%s_%s.%s", submission.getId(), flow, dropZoneInstanceName, userFileId,
          fileExtension);
      String thumbLocation = String.format("%s/%s_%s_%s-thumbnail.txt", submission.getId(), flow, dropZoneInstanceName,
          userFileId);

      cloudFileRepository.upload(uploadLocation, file);
      if (file.getContentType() != null && UserFile.isSupportedImage(file.getContentType())) {
        cloudFileRepository.upload(thumbLocation, thumbDataUrl);
      }

      UserFile uploadedFile = UserFile.builder()
          .submission_id(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize(UserFile.calculateFilesizeInMb(file.getSize()))
          .extension(file.getContentType()).build();

      Long newFileId = uploadedFileRepositoryService.save(uploadedFile);

      log.info("Created new file with id: " + newFileId);
      return ResponseEntity.status(HttpStatus.OK).body(newFileId);
    } catch (Exception e) {
      log.error("Error Occurred while uploading File " + e.getLocalizedMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
