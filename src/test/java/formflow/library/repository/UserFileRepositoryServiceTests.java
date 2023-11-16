package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeTypeUtils;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class UserFileRepositoryServiceTests {

  @Autowired
  private UserFileRepositoryService userFileRepositoryService;
  @Autowired
  private SubmissionRepositoryService submissionRepositoryService;

  @Value("${form-flow.uploads.default-doc-type-label:'OTHER'}")
  private String docTypeDefaultValue;

  private Submission submission;

  @BeforeEach
  void setup() {
    var inputData = Map.of(
        "testKey", "this is a test value",
        "otherTestKey", List.of("A", "B", "C")
    );
    submission = Submission.builder()
        .inputData(inputData)
        .urlParams(new HashMap<>())
        .flow("testFlow")
        .build();
    submission = submissionRepositoryService.save(submission);
  }

  @Test
  void shouldUseProperDefaultForDocTypeLabel() {
    UserFile testFile = UserFile.builder()
        .submission(submission)
        .originalName("originalName.jpg")
        .repositoryPath("/some/path/here")
        .filesize((float) 1000.0)
        .mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
        .virusScanned(true)
        .build();

    UserFile savedFile = saveAndReload(testFile);
    assertThat(savedFile.getDocTypeLabel()).isEqualTo(docTypeDefaultValue);
  }

  @Test
  void shouldUseDocTypeSetInUserFileBuilderCall() {
    UserFile testFile = UserFile.builder()
        .submission(submission)
        .originalName("originalName.jpg")
        .repositoryPath("/some/path/here")
        .filesize((float) 1000.0)
        .mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
        .virusScanned(true)
        .docTypeLabel("BirthCertificate")
        .build();

    UserFile savedFile = saveAndReload(testFile);
    assertThat(savedFile.getDocTypeLabel()).isEqualTo("BirthCertificate");
  }

  private UserFile saveAndReload(UserFile testFile) {
    UserFile savedFile = userFileRepositoryService.save(testFile);
    return userFileRepositoryService.findById(savedFile.getFileId()).get();
  }
}
