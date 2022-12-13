package formflow.library.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
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
  private CloudFileRepository cloudFileRepository;
  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;
  @MockBean
  private UserFileRepositoryService userFileRepositoryService;

  @Autowired
  private UploadController uploadController;
  Submission submission;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(uploadController).build();
    submission = Submission.builder().id(1L).build();
    super.setUp();
  }

  // TODO: deal with userFiles existing or not in the session then assert that the map on the session looks a certain way
  // TODO: Documentation?

  @Test
  public void fileUploadEndpointHitsCloudFileRepository() throws Exception {
    long fileId = 1L;
    when(userFileRepositoryService.save(any())).thenReturn(fileId);
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    doNothing().when(cloudFileRepository).upload(any(), any());
    MockMultipartFile testImage = new MockMultipartFile("file.jpeg", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
      .file("file", testImage.getBytes())
      .param("inputName", "testFile")
      .param("thumbDataURL", "base64string")
      .param("flow", "testFlow")
      .contentType(MediaType.APPLICATION_FORM_URLENCODED))
      .andExpect(status().is(200))
      .andExpect(content().string(String.valueOf(fileId)));

    verify(cloudFileRepository, times(1)).upload(any(), any());
  }

  @Test
  public void fileDeleteEndpointDeletesFromCloudRepositoryAndDb() throws Exception {
    UserFile testUserFile = UserFile.builder().submission_id(submission).build();
    when(submissionRepositoryService.findById(1L)).thenReturn(Optional.ofNullable(submission));
    when(userFileRepositoryService.findById(1L)).thenReturn(Optional.ofNullable(testUserFile));
    doNothing().when(cloudFileRepository).delete(any());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
            .param("returnPath", "foo")
            .param("inputName", "bar")
            .param("id", "1")
            .sessionAttr("id", 1L))
        .andExpect(status().is(302));

    verify(cloudFileRepository, times(1)).delete(any());
    verify(userFileRepositoryService, times(1)).deleteById(any());
  }

}
