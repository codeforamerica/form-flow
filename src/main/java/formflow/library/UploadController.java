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
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@EnableAutoConfiguration
@Slf4j
public class UploadController extends FormFlowController {

  private final UploadedFileRepositoryService uploadedFileRepositoryService;
  private final CloudFileRepository cloudFileRepository;
  private final ValidationService validationService;

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

      cloudFileRepository.upload(uploadLocation, file);

      UserFile uploadedFile = UserFile.builder()
          .submission_id(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize((float) file.getSize())
          .extension(file.getContentType()).build();

      Long newFileId = uploadedFileRepositoryService.save(uploadedFile);
      log.info("Created new file with id: " + newFileId);

      Map<String, HashMap<Long, HashMap<String, String>>> dzFilesMap =
          (Map<String, HashMap<Long, HashMap<String, String>>>) httpSession.getAttribute("userFiles");
      HashMap<Long, HashMap<String, String>> userFileMap = new HashMap<>();
      HashMap<String, String> fileInfo;

      if (dzFilesMap == null) {
        fileInfo = createFileInfo(uploadedFile, thumbDataUrl);
        httpSession.setAttribute("userFiles", Map.of(dropZoneInstanceName, userFileMap));
      } else {
        if (dzFilesMap.containsKey(dropZoneInstanceName)) {
          // User files exists, and it already has a key for this dropzone instance
          userFileMap = dzFilesMap.get(dropZoneInstanceName);
          fileInfo = createFileInfo(uploadedFile, thumbDataUrl);
        } else {
          // User files exists, but it doesn't have the dropzone instance yet
          userFileMap = new HashMap<>();
          fileInfo = createFileInfo(uploadedFile, thumbDataUrl);
          dzFilesMap.put(dropZoneInstanceName, userFileMap);
        }
      }
      userFileMap.put(uploadedFile.getFile_id(), fileInfo);

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
      HttpSession httpSession
  ) {
    try {
      log.info("\uD83D\uDD25 Try to delete: " + fileId);

      Long id = (Long) httpSession.getAttribute("id");
      Optional<Submission> maybeSubmission = submissionRepositoryService.findById(id);
      if (maybeSubmission.isPresent()) {
        Optional<UserFile> maybeFile = uploadedFileRepositoryService.findById(fileId);

        if (maybeFile.isPresent()) {
          UserFile file = maybeFile.get();
          if (id.equals(file.getSubmission_id().getId())) {
            log.info("Delete file {} from cloud storage", fileId);
            cloudFileRepository.delete(file.getRepositoryPath());

            if (file.getExtension() != null && UserFile.isSupportedImage(file.getExtension())) {
              String thumbnailPath = file.getRepositoryPath().replaceFirst("\\..*$", "-thumbnail.txt");
              log.info("Delete thumbnail for {} from cloud storage", fileId);
              cloudFileRepository.delete(thumbnailPath);
            }

            uploadedFileRepositoryService.deleteById(file.getFile_id());

            // TODO: delete from session
            // TODO: will need field name (dropZoneInstanceName) to be able to delete from session
            // TODO: will need to handle both the remove of one file from the array or removing the last item in the array, thus removing the array
            // httpSession.getAttribute("userFiles").get(inputName)
          } else {
            log.error(String.format("Submission %d does not match file %d's submission id %d", id, fileId,
                file.getSubmission_id().getId()));
            return new RedirectView("/error");
          }
        }
      } else {
        log.error(String.format("Session %d does not exist", id));
        return new RedirectView("/error");
      }

      return new RedirectView(returnPath);
    } catch (Exception e) {
      log.error("Error occurred while deleting file " + e.getLocalizedMessage());
      return new RedirectView("/error");
    }
  }

  /**
   * Creates a HashMap representation of an uploaded user file that holds information about the file (original file name, file
   * size, thumbnail and mime type) which we add to the session for persisting user file uploads when a user refreshes the page or
   * navigates away.
   *
   * @param userFile
   * @param thumbBase64String
   * @return Hashmap representation of a user file that includes original file name, file size, thumbnail as base64 encoded
   * string, and mime type.
   */
  private HashMap<String, String> createFileInfo(UserFile userFile, String thumbBase64String) {
    HashMap<String, String> fileInfo = new HashMap<>();
    fileInfo.put("originalFilename", userFile.getOriginalName());
    fileInfo.put("filesize", userFile.getFilesize().toString());
    fileInfo.put("thumbnailUrl", thumbBase64String);
    fileInfo.put("type", userFile.getExtension());
    return fileInfo;
  }
}
