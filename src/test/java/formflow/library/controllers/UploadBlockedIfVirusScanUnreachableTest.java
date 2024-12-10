package formflow.library.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.FileController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.file.ClammitVirusScanner;
import formflow.library.file.FileValidationService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@SpringBootTest(properties = {
    "form-flow.path=flows-config/test-conditional-navigation.yaml",
    "form-flow.uploads.virus-scanning.block-if-unreachable=true",
})
public class UploadBlockedIfVirusScanUnreachableTest extends AbstractMockMvcTest {

  private MockMvc mockMvc;
  @Autowired
  UserFileRepositoryService userFileRepositoryService;
  @Autowired
  private FileController fileController;
  @MockitoBean
  private ClammitVirusScanner clammitVirusScanner;
  @MockitoBean
  FileValidationService fileValidationService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    super.setUp();
  }

  @Test
  void shouldPreventUploadAndShowAnErrorIfBlockIfUnreachableIsSetToTrue() throws Exception {
    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    when(clammitVirusScanner.virusDetected(testImage)).thenThrow(
        new WebClientResponseException(500, "Failed!", null, null, null));
    when(fileValidationService.isAcceptedMimeType(testImage)).thenReturn(true);
    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("flow", "testFlow")
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .param("screen", "testUploadScreen")
            .session(session)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().is(HttpStatus.SERVICE_UNAVAILABLE.value()))
        .andExpect(content().string(
            this.messageSource.getMessage("upload-documents.error-virus-scanner-unavailable", null, Locale.ENGLISH)));
  }
}
