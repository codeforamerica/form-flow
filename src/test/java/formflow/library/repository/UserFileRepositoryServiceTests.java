package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepositoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeTypeUtils;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class UserFileRepositoryServiceTests {

  /*
  Note: These tests are disabled because the test db doesn't work like
  expected. Many of the fields are not getting their defaults set properly and so the
  default value for the field tested below is not getting populated. Until it is
  populated these tests will not function. Committing this code so we don't lose it.
  */
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
  @Disabled
  void shouldUseProperDefaultForDocTypeLabel() {
    UserFile testFile = UserFile.builder()
        .submission(submission)
        .originalName("originalName.jpg")
        .repositoryPath("/some/path/here")
        .filesize((float) 1000.0)
        .mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
        .virusScanned(true)
        .build();

    UserFile savedFile = userFileRepositoryService.save(testFile);
    assertThat(savedFile.getDocTypeLabel()).isEqualTo(docTypeDefaultValue);
  }

  @Test
  @Disabled
  void shouldUserDocTypeSetInUserFileBuilderCall() {
    UserFile testFile = UserFile.builder()
        .submission(submission)
        .originalName("originalName.jpg")
        .repositoryPath("/some/path/here")
        .filesize((float) 1000.0)
        .mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
        .virusScanned(true)
        .docTypeLabel("BirthCertificate")
        .build();

    UserFile savedFile = userFileRepositoryService.save(testFile);
    assertThat(savedFile.getDocTypeLabel()).isEqualTo("BirthCertificate");
  }
}
