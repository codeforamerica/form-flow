package formflow.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.lowagie.text.exceptions.BadPasswordException;
import com.lowagie.text.pdf.PdfReader;
import formflow.library.config.FlowConfiguration;
import formflow.library.config.FormFlowConfigurationProperties;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.CloudFile;
import formflow.library.file.CloudFileRepository;
import formflow.library.file.FileConversionService;
import formflow.library.file.FileValidationService;
import formflow.library.file.FileVirusScanner;
import formflow.library.utils.UserFileMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@EnableAutoConfiguration
@Slf4j
public class FileController extends FormFlowController {

  private final CloudFileRepository cloudFileRepository;
  private final Boolean blockIfClammitUnreachable;
  private final FileVirusScanner fileVirusScanner;
  private final FileValidationService fileValidationService;
  private final FileConversionService fileConversionService;
  private final String SESSION_USERFILES_KEY = "userFiles";
  private final Integer maxFiles;

  @Value("${form-flow.uploads.default-doc-type-label:}")
  private String defaultDocType;

  @Value("${form-flow.uploads.virus-scanning.enabled:false}")
  private boolean isVirusScanningEnabled;

  @Value("${form-flow.uploads.convert-to-pdf:false}")
  private boolean convertUploadToPDF;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public FileController(
      UserFileRepositoryService userFileRepositoryService,
      CloudFileRepository cloudFileRepository,
      FileVirusScanner fileVirusScanner,
      SubmissionRepositoryService submissionRepositoryService,
      List<FlowConfiguration> flowConfigurations,
      FormFlowConfigurationProperties formFlowConfigurationProperties,
      MessageSource messageSource,
      FileValidationService fileValidationService,
      FileConversionService fileConversionService,
      @Value("${form-flow.uploads.max-files:20}") Integer maxFiles,
      @Value("${form-flow.uploads.virus-scanning.block-if-unreachable:false}") boolean blockIfClammitUnreachable) {
    super(submissionRepositoryService, userFileRepositoryService, flowConfigurations, formFlowConfigurationProperties,
        messageSource);
    this.cloudFileRepository = cloudFileRepository;
    this.fileValidationService = fileValidationService;
    this.fileConversionService = fileConversionService;
    this.maxFiles = maxFiles;
    this.fileVirusScanner = fileVirusScanner;
    this.blockIfClammitUnreachable = blockIfClammitUnreachable;
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
   * <p>ON FAILURE: ResponseEntity with an error message and a status code.</p>
   */
  @PostMapping(value = "/file-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("flow") String flow,
      @RequestParam("inputName") String inputName,
      @RequestParam("thumbDataURL") String thumbDataUrl,
      @RequestParam("screen") String screen,
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

      Submission submission = findOrCreateSubmission(httpSession, flow);

      if (shouldRedirectDueToLockedSubmission(screen, submission,flow)) {
        log.info("The Submission for flow {} is locked. Cannot upload file.", flow);
        String message = messageSource.getMessage("upload-documents.locked-submission", null, locale);
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
      }
      
      UUID userFileId = UUID.randomUUID();
      if (submission.getId() == null) {
        submission.setFlow(flow);
        submission = saveToRepository(submission);
        setSubmissionInSession(httpSession, submission, flow);
      }

      if (!fileValidationService.isAcceptedMimeType(file)) {
        String message = messageSource.getMessage("upload-documents.error-mime-type", null, locale);
        return new ResponseEntity<>(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      }

      boolean wasScannedForVirus = false;
      if (isVirusScanningEnabled) {
        try {
          if (fileVirusScanner.virusDetected(file)) {
            String message = messageSource.getMessage("upload-documents.error-virus-found", null, locale);
            return new ResponseEntity<>(message, HttpStatus.UNPROCESSABLE_ENTITY);
          }
          wasScannedForVirus = true;
        } catch (WebClientResponseException | TimeoutException e) {
          wasScannedForVirus = false;
          if (blockIfClammitUnreachable) {
            log.error("The virus scan service could not be reached. Blocking upload.");
            String message = messageSource.getMessage("upload-documents.error-virus-scanner-unavailable", null, locale);
            return new ResponseEntity<>(message, HttpStatus.SERVICE_UNAVAILABLE);
          }
        }
      }

      if (fileValidationService.isTooLarge(file)) {
        String message = messageSource.getMessage("upload-documents.this-file-is-too-large",
            List.of(fileValidationService.getMaxFileSizeInMb()).toArray(),
            locale);
        return new ResponseEntity<>(message, HttpStatus.PAYLOAD_TOO_LARGE);
      }

      String fileExtension = Files.getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
      if (fileExtension.equals("pdf")) {
        try (PdfReader ignored = new PdfReader(file.getBytes())) {
        } catch (BadPasswordException e) {
          String message = messageSource.getMessage("upload-documents.error-password-protected", null, locale);
          return new ResponseEntity<>(message, HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException e) {
          String message = messageSource.getMessage("upload-documents.error-could-not-read-file", null, locale);
          return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }

      if (userFileRepositoryService.countOfUploadedFilesBySubmission(submission) >= maxFiles) {
        String message = messageSource.getMessage("upload-documents.error-maximum-number-of-files", null, locale);
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
      }
      String uploadLocation = String.format("%s/%s_%s_%s.%s", submission.getId(), flow, inputName, userFileId,
          fileExtension);

      cloudFileRepository.upload(uploadLocation, file);

      UserFile uploadedFile = UserFile.builder()
          .fileId(userFileId)
          .submission(submission)
          .originalName(file.getOriginalFilename())
          .repositoryPath(uploadLocation)
          .filesize((float) file.getSize())
          .mimeType(file.getContentType())
          .virusScanned(wasScannedForVirus)
          .docTypeLabel(defaultDocType)
          .build();

      uploadedFile = userFileRepositoryService.save(uploadedFile);
      log.info("Created new file with id: " + uploadedFile.getFileId());

      UserFileMap userFileMap = null;
      if (httpSession.getAttribute(SESSION_USERFILES_KEY) == null) {
        userFileMap = new UserFileMap();
      } else {
        userFileMap = objectMapper.readValue((String) httpSession.getAttribute(SESSION_USERFILES_KEY),
            UserFileMap.class);
      }

      userFileMap.addUserFileToMap(flow, inputName, uploadedFile, thumbDataUrl);
      httpSession.setAttribute(SESSION_USERFILES_KEY, objectMapper.writeValueAsString(userFileMap));

      if (convertUploadToPDF) {
        log.info("Converting upload {} to PDF", userFileId);
        // BEGIN CONVERSION
        MultipartFile convertedMultipartFile = fileConversionService.convertFileToPDF(file);

        String convertedFileExtension = Files.getFileExtension(
                Objects.requireNonNull(convertedMultipartFile.getOriginalFilename()));
        UUID convertedUserFileId = UUID.randomUUID();
        String convertedFileUploadLocation = String.format("%s/%s_%s_%s.%s", submission.getId(), flow, inputName,
                convertedUserFileId,
                convertedFileExtension);

        cloudFileRepository.upload(convertedFileUploadLocation, convertedMultipartFile);

        UserFile uploadedConvertedFile = UserFile.builder()
                .fileId(convertedUserFileId)
                .submission(submission)
                .originalName(convertedMultipartFile.getOriginalFilename())
                .repositoryPath(convertedFileUploadLocation)
                .filesize((float) convertedMultipartFile.getSize())
                .mimeType(convertedMultipartFile.getContentType())
                .virusScanned(true)
                .docTypeLabel(defaultDocType)
                .conversionSourceFileId(userFileId)
                .build();

        uploadedConvertedFile = userFileRepositoryService.save(uploadedConvertedFile);
        log.info("Created new converted file with id {} from original {}", uploadedConvertedFile.getFileId(), userFileId);
        // END CONVERSION
      }

      return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_PLAIN).body(uploadedFile.getFileId().toString());
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
   * @param flow                 The name of the current (active) flow
   * @param httpSession          The current HTTP session
   * @return ON SUCCESS: Returns a RedirectView to the returnPath
   * <p>ON FAILURE: Returns a RedirectView to the 'error' page</p>
   */
  @PostMapping("/file-delete")
  public RedirectView delete(
      @RequestParam("id") UUID fileId,
      @RequestParam("returnPath") String returnPath,
      @RequestParam("inputName") String dropZoneInstanceName,
      @RequestParam("flow") String flow,
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    try {
      log.info("POST delete (url: {}): fileId: {} inputName: {}", request.getRequestURI().toLowerCase(), fileId,
          dropZoneInstanceName);

      Submission submission = getSubmissionFromSession(httpSession, flow);
      if (submission == null) {
        log.error("Submission does not exist for file '{}', not deleting file", fileId);
        return new RedirectView("/error");
      }

      Optional<UserFile> maybeFile = userFileRepositoryService.findById(fileId);
      if (maybeFile.isEmpty()) {
        log.error("File with id '{}' not found. It may have already been deleted?", fileId);
        return new RedirectView("/error");
      }

      UserFile file = maybeFile.get();
      if (!submission.getId().equals(file.getSubmission().getId())) {
        log.error(
            String.format(
                "Submission %s does not match file %s's submission id %s",
                submission.getId(),
                fileId,
                file.getSubmission().getId()));
        return new RedirectView("/error");
      }

      log.info("Delete file {} from cloud storage", fileId);
      cloudFileRepository.delete(file.getRepositoryPath());
      userFileRepositoryService.deleteById(file.getFileId());

      UserFileMap userFileMap = objectMapper.readValue((String) httpSession.getAttribute(SESSION_USERFILES_KEY),
          UserFileMap.class);
      if (userFileMap == null) {
        log.error("User file map not set in session. Unable to update file information");
        throw new IndexOutOfBoundsException("Session does not contain user file mapping.");
      }
      userFileMap.removeUserFileFromMap(flow, fileId);
      httpSession.setAttribute(SESSION_USERFILES_KEY, objectMapper.writeValueAsString(userFileMap));

      return new RedirectView(returnPath);
    } catch (Exception e) {
      log.error("Error occurred while deleting file " + e.getLocalizedMessage());
      return new RedirectView("/error");
    }
  }

  /**
   * @param submissionId The submissionId of the file to be downloaded
   * @param fileId       The UUID of the file to be downloaded.
   * @param flow         The name of the current (active) flow
   * @param httpSession  The current HTTP session
   * @param request      The HttpServletRequest
   * @return ON SUCCESS: ResponseEntity with a response body that includes the file.
   * <p>ON FAILURE: A ResponseEntity returns an HTTP error code</p>
   */
  @GetMapping("/file-download/{flow}/{submissionId}/{fileId}")
  public ResponseEntity<StreamingResponseBody> downloadSingleFile(
      @PathVariable String submissionId,
      @PathVariable String fileId,
      @PathVariable String flow,
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    log.info("GET downloadSingleFile (url: {}): submissionId: {} fileId {}", request.getRequestURI().toLowerCase(), submissionId,
        fileId);

    if (!submissionId.equals(getSubmissionIdForFlow(httpSession, flow).toString())) {
      log.error("There was an attempt to download a file with submission ID '{}', " +
          "which does not match the submission of the file being downloaded.", submissionId);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Submission submission = getSubmissionFromSession(httpSession, flow);
    if (submission == null) {
      log.error("Submission does not exist for that file");
      return ResponseEntity.notFound().build();
    }

    Optional<UserFile> maybeFile = userFileRepositoryService.findById(UUID.fromString(fileId));
    if (maybeFile.isEmpty()) {
      log.error(String.format("Could not find the file with id: %s.", fileId));
      return ResponseEntity.notFound().build();
    }

    UserFile file = maybeFile.get();
    if (!submissionId.equals(file.getSubmission().getId().toString())) {
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
   * @param submissionId The submissionId of the all the files that you would like to download.
   * @param httpSession  The current HTTP session.
   * @param flow         The name of the current (active) flow
   * @param request      The HttpServletRequest
   * @return ON SUCCESS: ResponseEntity with a zip file containing all the files in a submission.
   * <p>ON FAILURE: ResponseEntity with a HTTP error message</p>
   */
  @GetMapping("/file-download/{flow}/{submissionId}")
  public ResponseEntity<StreamingResponseBody> downloadAllFiles(
      @PathVariable String submissionId,
      @PathVariable String flow,
      HttpSession httpSession,
      HttpServletRequest request
  ) {
    log.info("GET downloadAllFiles (url: {}): submissionId: {}", request.getRequestURI().toLowerCase(), submissionId);

    // first check to see if the ID in the session for the current flow is equal to the
    // requested submissionId in the URL path
    if (!submissionId.equals(getSubmissionIdForFlow(httpSession, flow).toString())) {
      log.error(
          "Attempted to download files belonging to submission " + submissionId + " but session id " + httpSession.getAttribute(
              "id") + " does not match.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // now check to see if the submission itself exists
    Submission submission = getSubmissionFromSession(httpSession, flow);
    if (submission == null) {
      log.error(String.format("The Submission %s was not found.", submissionId));
      return ResponseEntity.notFound().build();
    }

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
