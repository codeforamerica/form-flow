package formflow.library;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UploadedFileRepositoryService;
import formflow.library.upload.CloudFileRepository;
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

  public UploadController(
      UploadedFileRepositoryService uploadedFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      SubmissionRepositoryService submissionRepositoryService
  ) {
    super(submissionRepositoryService);
    this.uploadedFileRepositoryService = uploadedFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
  }

  @PostMapping("/file-upload")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("flow") String flow,
      HttpSession httpSession
  ) {
    try {
      log.info("You are in file upload endpoint");
      log.info("The file name is " + file.getOriginalFilename());
      Submission submission = submissionRepositoryService.findOrCreate(httpSession);
      UUID userFileId = UUID.randomUUID();
//      TODO: UserFileFactory.generate(file), then call userFile.getId() below
//      TODO: upload thumbnail.. maybe generatedid-thumbnail?

      // `submission` may not have been saved yet. If that's the case, the submission.getId() will return null.
      // If it's null, then save it to the db, so that we can get the submission's id to use in the
      // file object's path
      if (submission.getId() == null) {
        submission.setFlow(flow);
        saveToRepository(submission);
        httpSession.setAttribute("id", submission.getId());
      }

      log.info("submission " + submission);
      String uploadLocation = String.format("%s/%s", submission.getId(), userFileId);
      cloudFileRepository.upload(uploadLocation, file);

      //
      // TODO: save to user_files table
      // TODO: update input_data and save updated submission object
      // Once we merge code: we will need a unique identifier for the dropzone input widget to associated this with in the JSON
      // TODO: pass back new file id in response body
      //
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      log.error("Error Occurred while uploading File " + e.getLocalizedMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
