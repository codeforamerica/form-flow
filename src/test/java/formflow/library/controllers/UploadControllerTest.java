package formflow.library.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.UploadController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.upload.CloudFileRepository;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
public class UploadControllerTest extends AbstractMockMvcTest {

  // mock S endpoint
  // mock database
  // actual upload controller
  private MockMvc mockMvc;
  @SpyBean
  private CloudFileRepository s3CloudFileRepository;
  @SpyBean
  private SubmissionRepositoryService submissionRepositoryService;
  @SpyBean
  private UserFileRepositoryService userFileRepositoryService;

  @Autowired
  private UploadController uploadController;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(uploadController).build();
    super.setUp();
  }

  @Test
  public void testFileUpload() throws Exception {
    MockHttpSession mockHttpSession = new MockHttpSession();
    when(userFileRepositoryService.save(any())).thenReturn(1L);
//    when(session.getAttribute(anyString())).thenReturn(1L);
    doNothing().when(s3CloudFileRepository).upload(any(), any());
    MockMultipartFile testImage = new MockMultipartFile("file", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file(testImage)
            .param("inputName", "file")
            .param("flow", "testFlow")
            .sessionAttr("id", "1")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is(200));
    verify(s3CloudFileRepository, times(1)).upload(any(), any());
  }

  @Test
  public void testDeleteUploadedFile() throws Exception {
    Submission testSubmission = Submission.builder().id(1L).build();
    UserFile testUserFile = UserFile.builder().submission_id(testSubmission).build();
    when(submissionRepositoryService.findById(1L)).thenReturn(Optional.ofNullable(testSubmission));
    when(userFileRepositoryService.findById(1L)).thenReturn(Optional.ofNullable(testUserFile));
    doNothing().when(s3CloudFileRepository).delete(any());
    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
            .param("returnPath", "foo")
            .param("inputName", "bar")
            .param("id", "1"))
//            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is(302));
    verify(s3CloudFileRepository, times(1)).delete(any());
  }
}
