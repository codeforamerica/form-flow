package formflow.library.controllers;

import static formflow.library.FormFlowController.SUBMISSION_MAP_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import formflow.library.FileController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.ClammitVirusScanner;
import formflow.library.file.CloudFile;
import formflow.library.file.CloudFileRepository;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utils.UserFileMap;
import java.io.DataInput;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SpringBootTest(properties = {
    "form-flow.path=flows-config/test-conditional-navigation.yaml",
    "form-flow.uploads.max-file-size=1",
    "form-flow.uploads.max-files=10"
})
public class FileControllerTest extends AbstractMockMvcTest {

  Submission submission;
  private MockMvc mockMvc;
  @SpyBean
  private CloudFileRepository cloudFileRepository;
  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;
  @MockBean
  private UserFileRepositoryService userFileRepositoryService;
  @Autowired
  private FileController fileController;
  @MockBean
  private ClammitVirusScanner clammitVirusScanner;
  @Captor
  private ArgumentCaptor<UserFile> userFileArgumentCaptor;
  private final UUID fileId = UUID.randomUUID();

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    submission = Submission.builder().id(submissionUUID).build();

    when(clammitVirusScanner.virusDetected(any())).thenReturn(false);

    // Set the file ID on the UserFile since Mockito won't actually set one (it just returns what we tell it to)
    // It does not call the actual save method which is what sets the ID
    when(userFileRepositoryService.save(any())).thenAnswer(invocation -> {
      UserFile userFile = invocation.getArgument(0);
      userFile.setFileId(fileId);
      return fileId;
    });
    
    session.setAttribute(
        SUBMISSION_MAP_NAME,
        new HashMap<String, Object>(
            Map.of("testFlow", submission.getId())
        )
    );

    super.setUp();
  }

  @Test
  void shouldReturn404IfFlowDoesNotExist() throws Exception {
    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "flowThatDoesNotExist")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(404));
  }

  @Test
  public void fileUploadEndpointHitsCloudFileRepositoryAndAddsUserFileToSession() throws Exception {
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    doNothing().when(cloudFileRepository).upload(any(), any());
    // the "name" param has to match what the endpoint expects: "file"
    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    session = new MockHttpSession();

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().string(fileId.toString()));

    verify(cloudFileRepository, times(1)).upload(any(), any());
    
    ObjectMapper objectMapper = new ObjectMapper();
    UserFileMap userFileMap = objectMapper.readValue(session.getAttribute("userFiles").toString(), UserFileMap.class);
        
    
    // get the DZ Instance Map from the session and make sure the file info looks okay
    assertThat(userFileMap.getUserFileMap().size()).isEqualTo(1);
    assertThat(userFileMap.getUserFileMap().get("testFlow").size()).isEqualTo(1);

    UUID theNewFileId = (UUID) userFileMap.getUserFileMap().get("testFlow").get("dropZoneTestInstance").keySet().toArray()[0];
    Map<String, String> fileData = userFileMap.getUserFileMap().get("testFlow").get("dropZoneTestInstance").get(theNewFileId);
    assertThat(fileData.get("originalFilename")).isEqualTo("someImage.jpg");
    assertThat(fileData.get("filesize")).isEqualTo("4.0");
    assertThat(fileData.get("thumbnailUrl")).isEqualTo("base64string");
    assertThat(fileData.get("type")).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
  }

  @Test
  void shouldShowFileContainsVirusErrorIfClammitScanFindsVirus() throws Exception {
    MockMultipartFile testVirusFile = new MockMultipartFile(
        "file",
        "test-virus-file.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes());
    when(clammitVirusScanner.virusDetected(testVirusFile)).thenReturn(true);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testVirusFile)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
        .andExpect(content().string("We are unable to process this file because a virus was detected. Please try another file."));
  }

  @Test
  void shouldAllowUploadIfBlockIfUnreachableIsSetToFalse() throws Exception {
    doNothing().when(cloudFileRepository).upload(any(), any());

    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    when(clammitVirusScanner.virusDetected(testImage)).thenThrow(
        new WebClientResponseException(500, "Failed!", null, null, null));
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().string(fileId.toString()));

    verify(cloudFileRepository, times(1)).upload(any(), any());
  }

  @Test
  void shouldSetFalseIfVirusScannerDidNotRun() throws Exception {
    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    when(clammitVirusScanner.virusDetected(testImage)).thenThrow(
        new WebClientResponseException(500, "Failed!", null, null, null));
    doNothing().when(cloudFileRepository).upload(any(), any());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.OK.value()));

    verify(userFileRepositoryService).save(userFileArgumentCaptor.capture());
    assertThat(userFileArgumentCaptor.getValue().isVirusScanned()).isFalse();
  }

  @Test
  void shouldReturn413IfUploadedFileViolatesMaxFileSizeConstraint() throws Exception {
    MockMultipartFile testImage = new MockMultipartFile("file", "testFileSizeImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, new byte[(int) (FileUtils.ONE_MB + 1)]);

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.PAYLOAD_TOO_LARGE.value()))
        .andExpect(content().string("This file is too large and cannot be uploaded (max size: 1 MB)"));
  }

  @Test
  void shouldReturn4xxIfUploadFileViolatesMaxFilesConstraint() throws Exception {
    when(userFileRepositoryService.countBySubmission(submission)).thenReturn(10L);
    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(new MockMultipartFile("file", "testFileSizeImage.jpg",
                MediaType.IMAGE_JPEG_VALUE, new byte[10]))
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
        .andExpect(content().string(
            "You have uploaded the maximum number of files. You will have the opportunity to share more with a caseworker later."));
  }

  @Nested
  public class Delete {

    String dzWidgetInputName = "coolDzWidget";
    UserFile testUserFile = UserFile.builder()
        .submission(submission)
        .createdAt(Date.from(Instant.now()))
        .originalName("coolFile.jpg")
        .repositoryPath("pathToS3")
        .mimeType(".pdf")
        .filesize(Float.valueOf("10"))
        .virusScanned(false)
        .build();

    @BeforeEach
    void setUp() throws JsonProcessingException {
      UUID submissionUUID_1 = UUID.randomUUID();
      UUID submissionUUID_2 = UUID.randomUUID();
      submission = Submission.builder().id(submissionUUID_1).build();
      when(submissionRepositoryService.findById(submissionUUID_1)).thenReturn(Optional.ofNullable(submission));
      when(submissionRepositoryService.findById(submissionUUID_2)).thenReturn(Optional.ofNullable(submission));
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.ofNullable(testUserFile));
      doNothing().when(cloudFileRepository).delete(any());
      UserFileMap userFileMap = new UserFileMap();
      userFileMap.addUserFileToMap("testFlow", dzWidgetInputName, testUserFile, "thumbnail");
      session = new MockHttpSession();
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      ObjectMapper objectMapper = new ObjectMapper();
      session.setAttribute("userFiles", objectMapper.writeValueAsString(userFileMap));
    }

    @Test
    void endpointErrorsWhenSessionDoesntExist() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("flow", "testFlow")
              .param("id", fileId.toString()))
          .andExpect(status().is(HttpStatus.FOUND.value())).andExpect(redirectedUrl("/error"));
    }

    @Test
    void endpointErrorsWhenFileNotFoundInDb() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("flow", "testFlow")
              .param("id", UUID.randomUUID().toString())
              .session(session))
          .andExpect(status().is(HttpStatus.FOUND.value())).andExpect(redirectedUrl("/error"));
    }

    @Test
    void endpointErrorsWhenIdOnRequestDoesntMatchIdInDb() throws Exception {
      UUID submissionUUID = UUID.randomUUID();
      submission = Submission.builder().id(submissionUUID).build();
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", fileId.toString())
              .param("flow", "testFlow")
              .session(session))
          .andExpect(status().is(HttpStatus.FOUND.value())).andExpect(redirectedUrl("/error"));
    }

    @Test
    public void endpointDeletesFromCloudRepositoryDbAndSession() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", fileId.toString())
              .param("flow", "testFlow")
              .session(session))
          .andExpect(status().is(HttpStatus.FOUND.value()));
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.ofNullable(testUserFile));
      verify(cloudFileRepository, times(1)).delete(any());
      verify(userFileRepositoryService, times(1)).deleteById(any());
      assertThat(session.getAttribute("userFiles")).isEqualTo(new HashMap<>());
    }
  }

  @Nested
  public class Download {

    @Test
    void shouldReturnForbiddenStatusIfSessionIdDoesNotMatchSubmissionIdForSingleFileEndpoint() throws Exception {
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", UUID.randomUUID())
          )
      );
      UserFile userFile = UserFile.builder().submission(submission).build();
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.ofNullable(userFile));
      mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{flow}/{submissionId}/{fileId}", "testFlow", submission.getId().toString(), fileId)
                  .session(session))
          .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void shouldReturnForbiddenIfAFilesSubmissionIdDoesNotMatchSubmissionIdOnTheUserFile() throws Exception {
      Submission differentSubmissionIdFromUserFile = Submission.builder().id(UUID.randomUUID()).build();
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      UserFile userFile = UserFile.builder().submission(differentSubmissionIdFromUserFile)
          .fileId(fileId).build();
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.ofNullable(userFile));
      when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
      mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{flow}/{submissionId}/{fileId}", "testFlow", submission.getId().toString(), fileId)
                  .session(session))
          .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void shouldReturnForbiddenStatusIfSessionIdDoesNotMatchSubmissionIdForMultiFileEndpoint() throws Exception {
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", UUID.randomUUID())
          )
      );
      when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.ofNullable(submission));
      mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{flow}/{submissionId}", "testFlow", submission.getId().toString())
                  .session(session))
          .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void shouldReturnNotFoundIfSubmissionCanNotBeFoundForMultiFileEndpoint() throws Exception {
      UUID differentSubmissionId = UUID.randomUUID();
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", differentSubmissionId)
          )
      );
      mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{submissionId}", differentSubmissionId.toString())
                  .param("flow", "testFlow")
                  .session(session))
          .andExpect(status().is(404));
    }

    @Test
    void shouldReturnNotFoundIfSubmissionDoesNotContainAnyFiles() throws Exception {
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      when(userFileRepositoryService.findAllBySubmission(submission)).thenReturn(Collections.emptyList());
      when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.ofNullable(submission));
      mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{submissionId}", submission.getId().toString())
                  .param("flow", "testFlow")
                  .session(session))
          .andExpect(status().is(404));
    }

    @Test
    void singleFileEndpointShouldReturnNotFoundIfNoUserFileIsFoundForAGivenFileId() throws Exception {
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.empty());
      mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{flow}/{submissionId}/{fileId}", "testFlow", submission.getId().toString(), fileId)
                  .session(session))
          .andExpect(status().is(404));
    }

    @Test
    void singleFileEndpointShouldReturnTheSameFileBytesAsTheCloudFileRepository() throws Exception {
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      byte[] testFileBytes = "foo".getBytes();
      long fileSize = testFileBytes.length;
      CloudFile testcloudFile = new CloudFile(fileSize, testFileBytes);
      UserFile testUserFile = UserFile.builder()
          .originalName("testFileName")
          .mimeType("image/jpeg")
          .repositoryPath("testPath")
          .submission(submission)
          .build();
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.ofNullable(testUserFile));
      when(cloudFileRepository.get("testPath")).thenReturn(testcloudFile);
      when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{flow}/{submissionId}/{fileId}", "testFlow", submission.getId().toString(), fileId)
                  .session(session))
          .andExpect(MockMvcResultMatchers.request().asyncStarted())
          .andReturn();
      byte[] response = mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + testUserFile.getOriginalName() + "\""))
          .andReturn().getResponse().getContentAsByteArray();

      assertThat(Arrays.equals(testcloudFile.getFileBytes(), response)).isTrue();
    }

    @Test
    void multiFileEndpointShouldReturnZipOfUserFilesReturnedByTheCloudFileRepository() throws Exception {
      session.setAttribute(
          SUBMISSION_MAP_NAME,
          new HashMap<String, Object>(
              Map.of("testFlow", submission.getId())
          )
      );
      byte[] firstTestFileBytes = Files.readAllBytes(Paths.get("src/test/resources/test.png"));
      byte[] secondTestFileBytes = Files.readAllBytes(Paths.get("src/test/resources/test-platypus.gif"));
      long firstTestFileSize = firstTestFileBytes.length;
      long secondTestFileSize = secondTestFileBytes.length;
      CloudFile firstTestcloudFile = new CloudFile(firstTestFileSize, firstTestFileBytes);
      CloudFile secondTestcloudFile = new CloudFile(secondTestFileSize, secondTestFileBytes);

      UserFile firstTestUserFile = UserFile.builder()
          .originalName("test.png")
          .mimeType("image/png")
          .repositoryPath("testPath")
          .filesize((float) firstTestFileSize)
          .submission(submission)
          .build();

      UserFile secondTestUserFile = UserFile.builder()
          .originalName("test-platypus.gif")
          .mimeType("image/gif")
          .repositoryPath("testPath2")
          .filesize((float) secondTestFileSize)
          .submission(submission).build();

      List<UserFile> userFiles = Arrays.asList(firstTestUserFile, secondTestUserFile);
      when(userFileRepositoryService.findAllBySubmission(submission)).thenReturn(userFiles);

      when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.ofNullable(submission));
      when(cloudFileRepository.get("testPath")).thenReturn(firstTestcloudFile);
      when(cloudFileRepository.get("testPath2")).thenReturn(secondTestcloudFile);

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders
                  .get("/file-download/{flow}/{submissionId}", "testFlow", submission.getId().toString())
                  .session(session))
          .andExpect(MockMvcResultMatchers.request().asyncStarted())
          .andReturn();
      byte[] response = mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(mvcResult))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"UserFiles-" + submission.getId() + ".zip\""))
          .andReturn().getResponse().getContentAsByteArray();

      byte[] testZipFile = Files.readAllBytes(Paths.get("src/test/resources/test-archive.zip"));

      Map<String, byte[]> responseContents = getZipContentsMap(response);
      Map<String, byte[]> testZipContents = getZipContentsMap(testZipFile);

      assertThat(responseContents.keySet()).isEqualTo(testZipContents.keySet());

      for (String name : responseContents.keySet()) {
        assertThat(Arrays.equals(responseContents.get(name), testZipContents.get(name))).isTrue();
      }
    }
  }
}
