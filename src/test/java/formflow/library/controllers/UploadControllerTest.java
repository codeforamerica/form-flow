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

import formflow.library.UploadController;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import formflow.library.upload.CloudFileRepository;
import formflow.library.utilities.AbstractMockMvcTest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
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
public class UploadControllerTest extends AbstractMockMvcTest {

  Submission submission;
  private MockMvc mockMvc;
  @SpyBean
  private CloudFileRepository cloudFileRepository;
  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;
  @MockBean
  private UserFileRepositoryService userFileRepositoryService;
  @Autowired
  private UploadController uploadController;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(uploadController).build();
    submission = Submission.builder().id(1L).build();
    super.setUp();
  }

  @Test
  public void fileUploadEndpointDoesNotUploadPasswordProtectedPdf() throws Exception {
    // TODO: might can delete these first couple of when()s
    long fileId = 1L;
    when(userFileRepositoryService.save(any())).thenReturn(fileId);
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    doNothing().when(cloudFileRepository).upload(any(), any());
//    Old way
//    MockMultipartFile testPdf = new MockMultipartFile("file.pdf", "someImage.jpg",
//        MediaType.PDF, "test".getBytes());
    Path pdfPath = Paths.get("src/test/resources/password-protected.pdf");
    byte[] testPdf = Files.readAllBytes(pdfPath);
    session = new MockHttpSession();

    mockMvc.perform(MockMvcRequestBuilders.multipart("/file-upload")
            .file("file", testPdf)
            .param("inputName", "dropZoneTestInstance")
            .param("thumbDataURL", "base64string")
            .param("flow", "testFlow")
            .session(session)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is(415))
        // TODO: Unsupported media type, or something like that
        .andExpect(content().string("Something???"));

    verify(cloudFileRepository, times(0)).upload(any(), any());
  }

  @Test
  public void fileUploadEndpointHitsCloudFileRepositoryAndAddsUserFileToSession() throws Exception {
    long fileId = 1L;
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
        .andExpect(content().string(String.valueOf(fileId)));

    verify(cloudFileRepository, times(1)).upload(any(), any());
    UserFile testUserFile = new UserFile(
        1L,
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
    session.putValue("id", 1L);
    session.putValue("userFiles", testDzInstanceMap);

    assertThat(session.getAttribute("userFiles")).isEqualTo(testDzInstanceMap);
  }

  @Nested
  public class Delete {

    String dzWidgetInputName = "coolDzWidget";

    @BeforeEach
    void setUp() {
      UserFile testUserFile = UserFile.builder().submission_id(submission).build();
      when(submissionRepositoryService.findById(1L)).thenReturn(Optional.ofNullable(submission));
      when(submissionRepositoryService.findById(2L)).thenReturn(Optional.ofNullable(submission));
      when(userFileRepositoryService.findById(1L)).thenReturn(Optional.ofNullable(testUserFile));
      doNothing().when(cloudFileRepository).delete(any());
      HashMap<String, HashMap<Long, HashMap<String, String>>> dzWidgets = new HashMap<>();
      HashMap<Long, HashMap<String, String>> userFiles = new HashMap<>();
      userFiles.put(1L, UserFile.createFileInfo(
          new UserFile(
              1L,
              new Submission(),
              Date.from(Instant.now()),
              "coolFile.jpg",
              "pathToS3",
              ".pdf",
              Float.valueOf("10")
          ), "thumbnail"));
      dzWidgets.put(dzWidgetInputName, userFiles);
      session = new MockHttpSession();
      session.putValue("id", 1L);
      session.putValue("userFiles", dzWidgets);
    }

    @Test
    void endpointErrorsWhenSessionDoesntExist() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", "1"))
          .andExpect(status().is(302)).andExpect(redirectedUrl("/error"));
    }

    @Test
    void endpointErrorsWhenFileNotFoundInDb() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", "1000")
              .session(session))
          .andExpect(status().is(302)).andExpect(redirectedUrl("/error"));
    }

    @Test
    void endpointErrorsWhenIdOnRequestDoesntMatchIdInDb() throws Exception {
      submission = Submission.builder().id(2L).build();
      session.setAttribute("id", 2L);
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", "1")
              .session(session))
          .andExpect(status().is(302)).andExpect(redirectedUrl("/error"));
    }

    @Test
    public void endpointDeletesFromCloudRepositoryDbAndSession() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.multipart("/file-delete")
              .param("returnPath", "foo")
              .param("inputName", dzWidgetInputName)
              .param("id", "1")
              .session(session))
          .andExpect(status().is(302));

      verify(cloudFileRepository, times(1)).delete(any());
      verify(userFileRepositoryService, times(1)).deleteById(any());
      assertThat(session.getAttribute("userFiles")).isEqualTo(new HashMap<>());
    }
  }
}
