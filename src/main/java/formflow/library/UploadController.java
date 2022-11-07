package formflow.library;

import com.google.common.io.Files;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UploadedFileRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.upload.CloudFileRepository;
import java.util.ArrayList;
import java.util.Collections;
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

  public UploadController(
      UploadedFileRepositoryService uploadedFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      SubmissionRepositoryService submissionRepositoryService) {
    super(submissionRepositoryService);
    this.uploadedFileRepositoryService = uploadedFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
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
      String uploadLocation = String.format("%s/%s-%s.%s", submission.getId(), dropZoneInstanceName, userFileId,
          fileExtension);
      String thumbLocation = String.format("%s/%s-%s-thumbnail.txt", submission.getId(), dropZoneInstanceName, userFileId);
      cloudFileRepository.upload(uploadLocation, file);

      // TODO we need a way to figure out if this is the default image. Maybe just compare strings?
      if (file.getContentType() != null && UserFile.isSupportedImage(file.getContentType())) {
        cloudFileRepository.upload(thumbLocation, thumbDataUrl);
      }
      UserFile uploadedFile = UserFile.builder()
          .submission_id(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize(UserFile.calculateFilesizeInMb(file.getSize()))
          .extension(file.getContentType()).build();

      uploadedFileRepositoryService.save(uploadedFile);
      // TODO: update input_data and save updated submission object
      if (submission.getInputData().containsKey(dropZoneInstanceName)) {
        ArrayList<Long> userFiles = (ArrayList<Long>) submission.getInputData().get(dropZoneInstanceName);
        userFiles.add(uploadedFile.getFile_id());
      } else {
        submission.getInputData()
            .put(dropZoneInstanceName, new ArrayList<>(Collections.singletonList(uploadedFile.getFile_id())));
      }
      submissionRepositoryService.save(submission);
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
