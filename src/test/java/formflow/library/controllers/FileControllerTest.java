package formflow.library.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import formflow.library.FileController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.upload.CloudFileRepository;
import formflow.library.utilities.AbstractMockMvcTest;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-conditional-navigation.yaml"})
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

  private UUID fileId = UUID.randomUUID();

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    submission = Submission.builder().id(submissionUUID).build();
    super.setUp();
  }

  @Test
  public void fileUploadEndpointHitsCloudFileRepositoryAndAddsUserFileToSession() throws Exception {
    when(userFileRepositoryService.save(any())).thenReturn(fileId);
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    doNothing().when(cloudFileRepository).upload(any(), any());
    MockMultipartFile testImage = new MockMultipartFile("file.jpeg", "someImage.jpg",
        MediaType.IMAGE_JPEG_VALUE, "test".getBytes());
    session = new MockHttpSession();

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file("file", testImage.getBytes())
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .param("flow", "testFlow")
            .session(session)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is(200))
        .andExpect(content().string(fileId.toString()));

    verify(cloudFileRepository, times(1)).upload(any(), any());
    UserFile testUserFile = new UserFile(
        fileId,
        new Submission(),
        Date.from(Instant.now()),
        "coolFile.jpg",
        "pathToS3",
        ".pdf",
        Float.valueOf("10"));
    HashMap<String, HashMap<Long, HashMap<String, String>>> testDzInstanceMap = new HashMap<>();
    HashMap<Long, HashMap<String, String>> userFiles = new HashMap<>();
    userFiles.put(1L, UserFile.createFileInfo(testUserFile, "thumbnail"));
    testDzInstanceMap.put("dropZoneTestInstance", userFiles);
    session = new MockHttpSession();
    session.setAttribute("id", fileId);
    session.setAttribute("userFiles", testDzInstanceMap);

    assertThat(session.getAttribute("userFiles")).isEqualTo(testDzInstanceMap);
  }

  @Nested
  public class Delete {

    String dzWidgetInputName = "coolDzWidget";

    @BeforeEach
    void setUp() {
      // reset submission.
      UUID submissionUUID_1 = UUID.randomUUID();
      UUID submissionUUID_2 = UUID.randomUUID();
      submission = Submission.builder().id(submissionUUID_1).build();
      UserFile testUserFile = UserFile.builder().submissionId(submission).build();
      when(submissionRepositoryService.findById(submissionUUID_1)).thenReturn(Optional.ofNullable(submission));
      when(submissionRepositoryService.findById(submissionUUID_2)).thenReturn(Optional.ofNullable(submission));
      when(userFileRepositoryService.findById(fileId)).thenReturn(Optional.ofNullable(testUserFile));
      doNothing().when(cloudFileRepository).delete(any());
      HashMap<String, HashMap<UUID, HashMap<String, String>>> dzWidgets = new HashMap<>();
      HashMap<UUID, HashMap<String, String>> userFiles = new HashMap<>();
      userFiles.put(fileId, UserFile.createFileInfo(
          new UserFile(
              fileId,
              new Submission(),
              Date.from(Instant.now()),
              "coolFile.jpg",
              "pathToS3",
              ".pdf",
              Float.valueOf("10")
          ), "thumbnail"));
      dzWidgets.put(dzWidgetInputName, userFiles);
      session = new MockHttpSession();
      session.setAttribute("id", submission.getId());
      session.setAttribute("userFiles", dzWidgets);
    }

    @Test
    void endpointErrorsWhenSessionDoesntExist() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", fileId.toString()))
          .andExpect(status().is(302)).andExpect(redirectedUrl("/error"));
    }

    @Test
    void endpointErrorsWhenFileNotFoundInDb() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", UUID.randomUUID().toString())
              .session(session))
          .andExpect(status().is(302)).andExpect(redirectedUrl("/error"));
    }

    @Test
    void endpointErrorsWhenIdOnRequestDoesntMatchIdInDb() throws Exception {
      UUID submissionUUID = UUID.randomUUID();
      submission = Submission.builder().id(submissionUUID).build();
      session.setAttribute("id", submissionUUID);
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", fileId.toString())
              .session(session))
          .andExpect(status().is(302)).andExpect(redirectedUrl("/error"));
    }

    @Test
    public void endpointDeletesFromCloudRepositoryDbAndSession() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", fileId.toString())
              .session(session))
          .andExpect(status().is(302));

      verify(cloudFileRepository, times(1)).delete(any());
      verify(userFileRepositoryService, times(1)).deleteById(any());
      assertThat(session.getAttribute("userFiles")).isEqualTo(new HashMap<>());
    }
  }

  @Nested
  public class Download {

    //verify that when you hit the file download endpoint that only you can
    // download the zip file of your documents
    @Test
    void shouldReturnForbiddenStatusIfSessionIdDoesNotMatchSubmissionIdForSingleFile() {

    }

    @Test
    void shouldReturnForbiddenStatusIfSessionIdDoesNotMatchSubmissionId() throws Exception {
      //Submission
      //httpSession
//      UUID submissionUUID = UUID.randomUUID();
//      UserFile userFile1 = UserFile.builder()
//          .fileId(UUID.randomUUID()).build();
//
//      submission =  Submission.builder().id(submissionUUID).build();
      session.setAttribute("id", UUID.randomUUID());
      mockMvc.perform(MockMvcRequestBuilders.get("/file-download/{submissionId}", submission.getId().toString())
              .session(session))
          .andExpect(status().is(403));


    }
    //test that when you hit the endpoint only you can download a document from your submission
    //test that if generate a zip with your documents if you pass in a submission.
    //test that you generate a document when you pass in a submision and the userfile id.
  }

}