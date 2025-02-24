package formflow.library.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.FileController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.FileValidationService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {
    "form-flow.path=flows-config/test-conditional-navigation.yaml",
    "form-flow.uploads.virus-scanning.enabled=false",
})
public class NoOpVirusScannerTest extends AbstractMockMvcTest {

  private MockMvc mockMvc;
  private UUID fileUuid;
  @MockitoBean
  private SubmissionRepositoryService submissionRepositoryService;
  @MockitoBean
  private UserFileRepositoryService userFileRepositoryService;
  @Autowired
  private FileController fileController;
  @MockitoSpyBean
  FileValidationService fileValidationService;
  
  @Override
  @BeforeEach
  public void setUp() throws Exception {
    fileUuid = UUID.randomUUID();
    UUID submissionUUID = UUID.randomUUID();
    mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    Submission submission = Submission.builder().id(submissionUUID).build();

    setFlowInfoInSession(session, "testFlow", submission.getId());

    UserFile userFile = UserFile.builder()
        .fileId(UUID.randomUUID())
        .submission(submission)
        .fileId(fileUuid)
        .virusScanned(false)
        .originalName("testFile.jpg")
        .filesize(Float.valueOf("10.0"))
        .mimeType(MediaType.IMAGE_JPEG_VALUE)
        .repositoryPath("/foo")
        .build();

    when(submissionRepositoryService.findById(any())).thenReturn(Optional.of(submission));
    when(userFileRepositoryService.save(any())).thenReturn(userFile);
    super.setUp();
  }

  @Test
  void shouldBypassRealVirusScanningIfDisabled() throws Exception {
    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    when(fileValidationService.isAcceptedMimeType(testImage)).thenReturn(true);
    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .param("screen", "testUploadScreen")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().string(fileUuid.toString()));
  }
}
