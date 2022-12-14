package formflow.library;

import com.google.common.io.Files;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.upload.CloudFileRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.springframework.web.servlet.view.RedirectView;

@Controller
@EnableAutoConfiguration
@Slf4j
public class UploadController extends FormFlowController {

  private final UserFileRepositoryService uploadedFileRepositoryService;
  private final CloudFileRepository cloudFileRepository;
  private final ValidationService validationService;

  public UploadController(
      UserFileRepositoryService userFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      SubmissionRepositoryService submissionRepositoryService, ValidationService validationService) {
    super(submissionRepositoryService);
    this.uploadedFileRepositoryService = userFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
    this.validationService = validationService;
  }

  @PostMapping("/file-upload")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("flow") String flow,
      @RequestParam("inputName") String inputName,
      @RequestParam("thumbDataURL") String thumbDataUrl,
      HttpSession httpSession
  ) {
    try {
      // TODO This is not validating anything right now other than that you included the inputName in the corresponding inputs file
      HashMap<String, List<String>> errorMessages = validationService.validate(flow, inputName, file);
      Submission submission = submissionRepositoryService.findOrCreate(httpSession);
      UUID userFileId = UUID.randomUUID();
      if (submission.getId() == null) {
        submission.setFlow(flow);
        saveToRepository(submission);
        httpSession.setAttribute("id", submission.getId());
      }
      String fileExtension = Files.getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
      String uploadLocation = String.format("%s/%s_%s_%s.%s", submission.getId(), flow, inputName, userFileId,
          fileExtension);

      cloudFileRepository.upload(uploadLocation, file);

      UserFile uploadedFile = UserFile.builder()
          .submission_id(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize((float) file.getSize())
          .extension(file.getContentType()).build();

      Long newFileId = uploadedFileRepositoryService.save(uploadedFile);
      log.info("Created new file with id: " + newFileId);

      //TODO: change userFiles special string to constant to be referenced in thymeleaf
      HashMap<String, HashMap<Long, HashMap<String, String>>> dzFilesMap =
          (HashMap<String, HashMap<Long, HashMap<String, String>>>) httpSession.getAttribute("userFiles");
      HashMap<Long, HashMap<String, String>> userFileMap = new HashMap<>();
      HashMap<String, String> fileInfo;

      if (dzFilesMap == null) {
        fileInfo = UserFile.createFileInfo(uploadedFile, thumbDataUrl);
        HashMap<String, HashMap<Long, HashMap<String, String>>> dropzoneInstanceMap = new HashMap<>();
        dropzoneInstanceMap.put(inputName, userFileMap);
        httpSession.setAttribute("userFiles", dropzoneInstanceMap);
      } else {
        if (dzFilesMap.containsKey(inputName)) {
          // User files exists, and it already has a key for this dropzone instance
          userFileMap = dzFilesMap.get(inputName);
          fileInfo = UserFile.createFileInfo(uploadedFile, thumbDataUrl);
        } else {
          // User files exists, but it doesn't have the dropzone instance yet
          userFileMap = new HashMap<>();
          fileInfo = UserFile.createFileInfo(uploadedFile, thumbDataUrl);
          dzFilesMap.put(inputName, userFileMap);
        }
      }
      userFileMap.put(newFileId, fileInfo);

      return ResponseEntity.status(HttpStatus.OK).body(newFileId);
    } catch (Exception e) {
      log.error("Error occurred while uploading file " + e.getLocalizedMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/file-delete")
  RedirectView delete(
      @RequestParam("id") Long fileId,
      @RequestParam("returnPath") String returnPath,
      @RequestParam("inputName") String dropZoneInstanceName,
      HttpSession httpSession
  ) {
    try {
      log.info("\uD83D\uDD25 Try to delete: " + fileId);

      Long submissionId = (Long) httpSession.getAttribute("id");
      Optional<Submission> maybeSubmission = submissionRepositoryService.findById(submissionId);

      if (maybeSubmission.isEmpty()) {
        log.error(String.format("Session %d does not exist", submissionId));
        return new RedirectView("/error");
      }

      Optional<UserFile> maybeFile = uploadedFileRepositoryService.findById(fileId);
      if (maybeFile.isEmpty()) {
        log.error(String.format("File with id %d may have already been deleted", fileId));
        return new RedirectView("/error");
      }

      UserFile file = maybeFile.get();
      if (!submissionId.equals(file.getSubmission_id().getId())) {
        log.error(String.format("Submission %d does not match file %d's submission id %d", submissionId, fileId,
            file.getSubmission_id().getId()));
        return new RedirectView("/error");
      }

      log.info("Delete file {} from cloud storage", fileId);
      cloudFileRepository.delete(file.getRepositoryPath());
      uploadedFileRepositoryService.deleteById(file.getFile_id());
      HashMap<String, HashMap<Long, HashMap<String, String>>> dzFilesMap =
          (HashMap<String, HashMap<Long, HashMap<String, String>>>) httpSession.getAttribute("userFiles");
      HashMap<Long, HashMap<String, String>> userFileMap = dzFilesMap.get(dropZoneInstanceName);

      userFileMap.remove(fileId);
      if (userFileMap.isEmpty()) {
        dzFilesMap.remove(dropZoneInstanceName);
      }

      return new RedirectView(returnPath);
    } catch (Exception e) {
      log.error("Error occurred while deleting file " + e.getLocalizedMessage());
      return new RedirectView("/error");
    }
  }
}
