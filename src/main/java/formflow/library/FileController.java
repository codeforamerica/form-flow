package formflow.library;

import com.google.common.io.Files;
import formflow.library.config.FlowConfiguration;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.CloudFile;
import formflow.library.file.CloudFileRepository;
import formflow.library.file.FileValidationService;
import formflow.library.file.FileVirusScanner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@EnableAutoConfiguration
@Slf4j
public class FileController extends FormFlowController {

  private final UserFileRepositoryService userFileRepositoryService;
  private final CloudFileRepository cloudFileRepository;

  @Value("${form-flow.uploads.virus-scanning.enabled:false}")
  private Boolean shouldScanForViruses;

  @Value("${form-flow.uploads.virus-scanning.block-if-unreachable:false}")
  private Boolean blockIfClammitCannotBeReached;

  private final FileVirusScanner fileVirusScanner;

  private final MessageSource messageSource;
  private final FileValidationService fileValidationService;
  private final String SESSION_USERFILES_KEY = "userFiles";
  private final Integer maxFiles;

  public FileController(
      UserFileRepositoryService userFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      @Autowired(required = false)
      FileVirusScanner fileVirusScanner,
      SubmissionRepositoryService submissionRepositoryService,
      List<FlowConfiguration> flowConfigurations,
      MessageSource messageSource,
      FileValidationService fileValidationService,
      @Value("${form-flow.uploads.max-files}") Integer maxFiles) {
    super(submissionRepositoryService, flowConfigurations);
    this.userFileRepositoryService = userFileRepositoryService;
    this.cloudFileRepository = cloudFileRepository;
    this.messageSource = messageSource;
    this.fileValidationService = fileValidationService;
    this.maxFiles = maxFiles;
    this.fileVirusScanner = fileVirusScanner;
  }

  /**
   * File upload endpoint.
   *
   * @param file         A MultipartFile file
   * @param flow         The current flow name
   * @param inputName    The current inputName
   * @param thumbDataUrl The thumbnail URL generated from the upload
   * @param httpSession  The current HTTP session
   * @return ON SUCCESS: ResponseEntity with a body containing the id of a file. body.
   * <p>ON FAILURE: RepsonseEntity with an error message and a status code.</p>
   */
  @PostMapping(value = "/file-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("flow") String flow,
      @RequestParam("inputName") String inputName,
      @RequestParam("thumbDataURL") String thumbDataUrl,
      HttpSession httpSession,
      HttpServletRequest request,
      Locale locale
  ) {
    log.info("POST upload (url: {}): flow: {}, inputName: {}", request.getRequestURI().toLowerCase(), flow, inputName);
    try {
      if (!doesFlowExist(flow)) {
        throwNotFoundError(flow, null,
            String.format("Could not find flow with name %s in your application's flow configuration.", flow));
      }

      Submission submission = submissionRepositoryService.findOrCreate(httpSession);
      UUID userFileId = UUID.randomUUID();
      if (submission.getId() == null) {
        submission.setFlow(flow);
        saveToRepository(submission);
        httpSession.setAttribute("id", submission.getId());
      }

      if (!fileValidationService.isAcceptedMimeType(file)) {
        String message = messageSource.getMessage("upload-documents.error-mime-type", null, locale);
        return new ResponseEntity<>(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      }
      
      boolean wasScannedForVirus = true;
      try {
        if (shouldScanForViruses && fileVirusScanner.virusDetected(file)) {
          String message = messageSource.getMessage("upload-documents.error-virus-found", null, locale);
          return new ResponseEntity<>(message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
      } catch (Exception e) {
        if (blockIfClammitCannotBeReached) {
          log.error("The virus scan service could not be reached. Blocking upload.");
          String message = messageSource.getMessage("upload-documents.error-virus-scanner-unavailable", null, locale);
          return new ResponseEntity<>(message, HttpStatus.SERVICE_UNAVAILABLE);
        }
        wasScannedForVirus = false;
      }

      if (fileValidationService.isTooLarge(file)) {
        String message = messageSource.getMessage("upload-documents.this-file-is-too-large",
            List.of(fileValidationService.getMaxFileSizeInMb()).toArray(),
            locale);
        return new ResponseEntity<>(message, HttpStatus.PAYLOAD_TOO_LARGE);
      }

      String fileExtension = Files.getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
      if (fileExtension.equals("pdf")) {
        try (PDDocument ignored = Loader.loadPDF(file.getBytes())) {
        } catch (InvalidPasswordException e) {
          // TODO update when we add internationalization to use locale for message source
          String message = messageSource.getMessage("upload-documents.error-password-protected", null, locale);
          return new ResponseEntity<>(message, HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException e) {
          String message = messageSource.getMessage("upload-documents.error-could-not-read-file", null, locale);
          return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      if (userFileRepositoryService.countBySubmission(submission) >= maxFiles) {
        String message = messageSource.getMessage("upload-documents.error-maximum-number-of-files", null, locale);
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
      }
      String uploadLocation = String.format("%s/%s_%s_%s.%s", submission.getId(), flow, inputName, userFileId,
          fileExtension);

      cloudFileRepository.upload(uploadLocation, file);

      UserFile uploadedFile = UserFile.builder()
          .submission(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize((float) file.getSize())
          .mimeType(file.getContentType())
          .virusScanned(wasScannedForVirus)
          .build();

      UUID newFileId = userFileRepositoryService.save(uploadedFile);
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
          userFileMap = dzFilesMap.get(inputName);
          // Double check that files in session cookie are in db
          userFileMap.entrySet().removeIf(e -> userFileRepositoryService.findById(e.getKey()).isEmpty());
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
      if (e instanceof ResponseStatusException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
      }
      log.error("Error occurred while uploading file: " + e.getLocalizedMessage());
      String message = messageSource.getMessage("upload-documents.file-upload-error", null, locale);
      return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @param fileId               The id of an uploaded file that should be deleted
   * @param returnPath           The path to the page that they came from
   * @param dropZoneInstanceName The drop zone instance used to get the user file name
   * @param httpSession          The current HTTP session
   * @return ON SUCCESS: Returns a RedirectView to the returnPath
   * <p>ON FAILURE: Returns a RedirectView to the 'error' page</p>
   */
  @PostMapping("/file-delete")
  RedirectView delete(
      @RequestParam("id") UUID fileId,
      @RequestParam("returnPath") String returnPath,
      @RequestParam("inputName") String dropZoneInstanceName,
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    try {
      log.info("POST delete (url: {}): fileId: {} inputName: {}", request.getRequestURI().toLowerCase(), fileId,
          dropZoneInstanceName);
      UUID submissionId = (UUID) httpSession.getAttribute("id");
      Optional<Submission> maybeSubmission = submissionRepositoryService.findById(submissionId);

      if (maybeSubmission.isEmpty()) {
        log.error(String.format("Submission %s does not exist", submissionId.toString()));
        return new RedirectView("/error");
      }

      Optional<UserFile> maybeFile = userFileRepositoryService.findById(fileId);
      if (maybeFile.isEmpty()) {
        log.error(String.format("File with id %s may have already been deleted", fileId));
        return new RedirectView("/error");
      }

      UserFile file = maybeFile.get();
      if (!submissionId.equals(file.getSubmission().getId())) {
        log.error(String.format("Submission %s does not match file %s's submission id %s", submissionId, fileId,
            file.getSubmission().getId()));
        return new RedirectView("/error");
      }

      log.info("Delete file {} from cloud storage", fileId);
      cloudFileRepository.delete(file.getRepositoryPath());
      userFileRepositoryService.deleteById(file.getFileId());
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

  /**
   * @param httpSession  The current HTTP session
   * @param submissionId The submissionId of the file to be downloaded
   * @param fileId       The UUID of the file to be downloaded.
   * @return ON SUCCESS: ResponseEntity with a response body that includes the file.
   * <p>ON FAILURE: A ResponseEntity returns an HTTP error code</p>
   */
  @GetMapping("/file-download/{submissionId}/{fileId}")
  public ResponseEntity<StreamingResponseBody> downloadSingleFile(
      HttpSession httpSession,
      @PathVariable String submissionId,
      @PathVariable String fileId,
      HttpServletRequest request
  ) {
    log.info("GET downloadSingleFile (url: {}): submissionId: {} fileId {}", request.getRequestURI().toLowerCase(), submissionId,
        fileId);
    if (!submissionId.equals(httpSession.getAttribute("id").toString())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Optional<UserFile> maybeFile = userFileRepositoryService.findById(UUID.fromString(fileId));
    if (maybeFile.isEmpty()) {
      log.error(String.format("Could not find the file with id: %s.", fileId));
      return ResponseEntity.notFound().build();
    }

    UserFile file = maybeFile.get();

    if (!httpSession.getAttribute("id").toString().equals(file.getSubmission().getId().toString())) {
      log.error(String.format("Attempt to download file with submission ID %s but session ID %s does not match",
          file.getSubmission().getId(), httpSession.getAttribute("id")));
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

  /**
   * @param httpSession  The current HTTP session.
   * @param submissionId The submissionId of the all the files that you would like to download.
   * @return ON SUCCESS: ResponseEntity with a zip file containing all the files in a submission.
   * <p>ON FAILURE: ResponseEntity with a HTTP error message</p>
   */
  @GetMapping("/file-download/{submissionId}")
  ResponseEntity<StreamingResponseBody> downloadAllFiles(
      HttpSession httpSession,
      @PathVariable String submissionId,
      HttpServletRequest request
  ) {
    log.info("GET downloadAllFiles (url: {}): submissionId: {}", request.getRequestURI().toLowerCase(), submissionId);
    if (!httpSession.getAttribute("id").toString().equals(submissionId)) {
      log.error(
          "Attempted to download files belonging to submission " + submissionId + " but session id " + httpSession.getAttribute(
              "id") + " does not match.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Optional<Submission> maybeSubmission = submissionRepositoryService.findById(UUID.fromString(submissionId));
    if (maybeSubmission.isEmpty()) {
      log.error(String.format("The Submission %s was not found.", submissionId));
      return ResponseEntity.notFound().build();
    }
    Submission submission = maybeSubmission.get();

    List<UserFile> userFiles = userFileRepositoryService.findAllBySubmission(submission);

    if (userFiles.isEmpty()) {
      log.error("No files belonging to submission " + submissionId + " were found.");
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
  }
}
