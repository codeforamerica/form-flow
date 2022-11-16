package formflow.library;

import com.google.common.io.Files;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UploadedFileRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.upload.CloudFileRepository;
import java.util.ArrayList;
import java.util.Collections;
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
      String uploadLocation = String.format("%s/%s-%s.%s", submission.getId(), dropZoneInstanceName, userFileId,
          fileExtension);
      String thumbLocation = String.format("%s/%s-%s-thumbnail.txt", submission.getId(), dropZoneInstanceName, userFileId);

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

      uploadedFileRepositoryService.save(uploadedFile);

      // 1. is in a subflow
      //   2.  Does inputName already exist in submission?
      //   3. inputName doesn't exist, put file
      // 2. not in a subflow
      //   3. inputName doesn't exist, put file
      // 4. inputName doesn't exist in subflow, put file

      Map<String, Object> inputData = submission.getInputData();

      if (subflow != null) {
        if (inputData.containsKey(subflow)) {
          Map<String, Object> subflowData = (Map<String, Object>) inputData.get(subflow);
          if (subflowData.containsKey(dropZoneInstanceName)) {

            HashMap<String, ArrayList<Long>> userFiles = (HashMap<String, ArrayList<Long>>) subflowData
                .get(dropZoneInstanceName);
            userFiles.get(USER_FILE_ID_KEY).add(uploadedFile.getFile_id());
          } else {
            HashMap<String, ArrayList<Long>> userFiles = new HashMap<>();
            userFiles.put(USER_FILE_ID_KEY, new ArrayList<>(Collections.singletonList(uploadedFile.getFile_id())));
            subflowData.put(dropZoneInstanceName, userFiles);
          }
        } else {
          // add subflow... in the case of the first screen of a subflow
          HashMap<String, ArrayList<Long>> userFiles = new HashMap<>();
          userFiles.put(USER_FILE_ID_KEY, new ArrayList<>(Collections.singletonList(uploadedFile.getFile_id())));

          HashMap<String, Object> newSubflowWithFiles = new HashMap<>();
          newSubflowWithFiles.put(dropZoneInstanceName, userFiles);
          inputData.put(subflow, newSubflowWithFiles);
        }
      } else {
        // are there files already associated with this submission?
        if (submission.getInputData().containsKey(dropZoneInstanceName)) {
          // yes, there are already files, add this one on
          HashMap<String, ArrayList<Long>> userFiles = (HashMap<String, ArrayList<Long>>) submission.getInputData()
              .get(dropZoneInstanceName);
          userFiles.get(USER_FILE_ID_KEY).add(uploadedFile.getFile_id());
        } else {
          // no, there are no files, create the array and add this as the first one
          HashMap<String, ArrayList<Long>> userFiles = new HashMap<>();
          userFiles.put(USER_FILE_ID_KEY, new ArrayList<>(Collections.singletonList(uploadedFile.getFile_id())));
          submission.getInputData().put(dropZoneInstanceName, userFiles);
        }
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
