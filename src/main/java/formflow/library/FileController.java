package formflow.library;

import com.google.common.io.Files;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.upload.CloudFile;
import formflow.library.upload.CloudFileRepository;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@EnableAutoConfiguration
@Slf4j
public class FileController extends FormFlowController {

  private final UserFileRepositoryService uploadedFileRepositoryService;
  private final CloudFileRepository cloudFileRepository;

  private final MessageSource messageSource;

  private final String SESSION_USERFILES_KEY = "userFiles";

  public FileController(
      UserFileRepositoryService userFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      SubmissionRepositoryService submissionRepositoryService,
      MessageSource messageSource) {
    super(submissionRepositoryService);
    this.uploadedFileRepositoryService = userFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
    this.messageSource = messageSource;
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
      Submission submission = submissionRepositoryService.findOrCreate(httpSession);
      UUID userFileId = UUID.randomUUID();
      if (submission.getId() == null) {
        submission.setFlow(flow);
        saveToRepository(submission);
        httpSession.setAttribute("id", submission.getId());
      }
      String fileExtension = Files.getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));

      if (fileExtension.equals("pdf")) {
        try (PDDocument pdfFile = PDDocument.load(file.getInputStream())) {
        } catch (InvalidPasswordException e) {
          // TODO update when we add internationalization to use locale for message source
          String message = messageSource.getMessage("upload-documents.error-password-protected", null, null);
          return new ResponseEntity<>(message, HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException e) {
          String message = messageSource.getMessage("upload-documents.error-could-not-read-file", null, null);
          return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      String uploadLocation = String.format("%s/%s_%s_%s.%s", submission.getId(), flow, inputName, userFileId,
          fileExtension);

      cloudFileRepository.upload(uploadLocation, file);

      UserFile uploadedFile = UserFile.builder()
          .submissionId(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize((float) file.getSize())
          .mimeType(file.getContentType()).build();

      UUID newFileId = uploadedFileRepositoryService.save(uploadedFile);
      log.info("Created new file with id: " + newFileId);

      //TODO: change userFiles special string to constant to be referenced in thymeleaf
      HashMap<String, HashMap<UUID, HashMap<String, String>>> dzFilesMap;
      HashMap<UUID, HashMap<String, String>> userFileMap;
      HashMap<String, String> fileInfo = UserFile.createFileInfo(uploadedFile, thumbDataUrl);

      if (httpSession.getAttribute(SESSION_USERFILES_KEY) == null) {
        // no dropzone data exists at all yet, let's create space for the session map as well
        // as for the current file being uploaded
        dzFilesMap = new HashMap<>();
        userFileMap = new HashMap<>();
      } else {
        dzFilesMap = (HashMap<String, HashMap<UUID, HashMap<String, String>>>) httpSession.getAttribute(SESSION_USERFILES_KEY);
        if (dzFilesMap.containsKey(inputName)) {
          // a map for this dropzone widget already exists, let's add more files to it
          userFileMap = dzFilesMap.get(inputName);
        } else {
          // a map for this inputName dropzone instance does not exist yet, let's create it so we can add files to it
          userFileMap = new HashMap<>();
        }
      }

      userFileMap.put(newFileId, fileInfo);
      dzFilesMap.put(inputName, userFileMap);
      httpSession.setAttribute(SESSION_USERFILES_KEY, dzFilesMap);

      return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(newFileId.toString());
    } catch (Exception e) {
      log.error("Error occurred while uploading file " + e.getLocalizedMessage());
      // TODO update when we add internationalization to use locale for message source
      String message = messageSource.getMessage("upload-documents.file-upload-error", null, null);
      return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/file-delete")
  RedirectView delete(
      @RequestParam("id") UUID fileId,
      @RequestParam("returnPath") String returnPath,
      @RequestParam("inputName") String dropZoneInstanceName,
      HttpSession httpSession
  ) {
    try {
      log.info("\uD83D\uDD25 Try to delete: " + fileId);

      UUID submissionId = (UUID) httpSession.getAttribute("id");
      Optional<Submission> maybeSubmission = submissionRepositoryService.findById(submissionId);

      if (maybeSubmission.isEmpty()) {
        log.error(String.format("Session %d does not exist", submissionId));
        return new RedirectView("/error");
      }

      Optional<UserFile> maybeFile = uploadedFileRepositoryService.findById(fileId);
      if (maybeFile.isEmpty()) {
        log.error(String.format("File with id %s may have already been deleted", fileId));
        return new RedirectView("/error");
      }

      UserFile file = maybeFile.get();
      if (!submissionId.equals(file.getSubmissionId().getId())) {
        log.error(String.format("Submission %d does not match file %s's submission id %d", submissionId, fileId,
            file.getSubmissionId().getId()));
        return new RedirectView("/error");
      }

      log.info("Delete file {} from cloud storage", fileId);
      cloudFileRepository.delete(file.getRepositoryPath());
      uploadedFileRepositoryService.deleteById(file.getFileId());
      HashMap<String, HashMap<UUID, HashMap<String, String>>> dzFilesMap =
          (HashMap<String, HashMap<UUID, HashMap<String, String>>>) httpSession.getAttribute(SESSION_USERFILES_KEY);
      HashMap<UUID, HashMap<String, String>> userFileMap = dzFilesMap.get(dropZoneInstanceName);

      userFileMap.remove(fileId);
      if (userFileMap.isEmpty()) {
        dzFilesMap.remove(dropZoneInstanceName);
      }

      httpSession.setAttribute(SESSION_USERFILES_KEY, dzFilesMap);

      return new RedirectView(returnPath);
    } catch (Exception e) {
      log.error("Error occurred while deleting file " + e.getLocalizedMessage());
      return new RedirectView("/error");
    }
  }

  @GetMapping("/file-download/{submissionId}/{fileId}")
  public ResponseEntity<StreamingResponseBody> downloadSingleFile(
      HttpSession httpSession,
      @PathVariable String submissionId,
      @PathVariable String fileId
  ) {

    Optional<UserFile> maybeFile = uploadedFileRepositoryService.findById(UUID.fromString(fileId));
    if (maybeFile.isEmpty()) {
      log.error(String.format("File with id %s may have already been deleted", fileId));
      return ResponseEntity.notFound().build();
    }

    UserFile file = maybeFile.get();
    if (!submissionId.equals(httpSession.getAttribute("id").toString())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (!httpSession.getAttribute("id").toString().equals(file.getSubmissionId().getId().toString())) {
      log.error(String.format("Attempt to download file with submission ID %s but session ID %s does not match",
          file.getSubmissionId().getId(), httpSession.getAttribute("id")));
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    String repositoryPath = file.getRepositoryPath();
    String filename = file.getOriginalName();
    String contentType = file.getMimeType();

    CloudFile cloudFile = cloudFileRepository.get(repositoryPath);
    byte[] fileData = cloudFile.getFileBytes();
    long fileSize = cloudFile.getFileSize();

    StreamingResponseBody responseBody = outputStream -> {
      try {
        outputStream.write(fileData);
      } catch (IOException e) {
        log.error("Error occurred while downloading file " + e.getMessage());
      }
    };

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType(contentType))
        .contentLength(fileSize)
        .body(responseBody);
  }

  @GetMapping("/file-download/{submissionId}")
  ResponseEntity<StreamingResponseBody> downloadAllFiles(
      HttpSession httpSession,
      @PathVariable String submissionId
  ) {
    Optional<Submission> maybeSubmission = submissionRepositoryService.findById(UUID.fromString(submissionId));
    if (httpSession.getAttribute("id").toString().equals(submissionId) && maybeSubmission.isPresent()) {
      if (maybeSubmission.isEmpty()) {
        log.error(String.format("Session %d does not exist", submissionId));
        return ResponseEntity.notFound().build();
      }

      Submission submission = maybeSubmission.get();
      List<UserFile> userFiles = uploadedFileRepositoryService.findAllBySubmissionId(submission);
      if (userFiles.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        for (UserFile userFile : userFiles) {
          ZipEntry fileEntry = new ZipEntry(userFile.getOriginalName());
          fileEntry.setSize(userFile.getFilesize().longValue());
          zos.putNextEntry(fileEntry);

          CloudFile cloudFile = cloudFileRepository.get(userFile.getRepositoryPath());
          byte[] fileBytes = cloudFile.getFileBytes();
          zos.write(fileBytes);
          zos.closeEntry();
        }
      } catch (IOException e) {
        log.error("Error occurred while downloading file " + e.getMessage());
        return ResponseEntity.internalServerError().build();
      }

      StreamingResponseBody responseBody = outputStream -> {
        baos.writeTo(outputStream);
        baos.close();
      };

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "UserFiles-" + submission.getId() + ".zip" + "\"")
          .body(responseBody);
    } else {
      log.error(
          "Attempted to download files belonging to submission " + submissionId + " but session id " + httpSession.getAttribute(
              "id") + " does not match.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }
}