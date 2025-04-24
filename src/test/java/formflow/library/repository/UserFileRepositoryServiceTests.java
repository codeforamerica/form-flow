package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepository;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.data.UserFile;
import formflow.library.data.UserFileRepository;
import formflow.library.data.UserFileRepositoryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

    @Autowired
    private UserFileRepository userFileRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Value("${form-flow.uploads.default-doc-type-label:'OTHER'}")
    private String docTypeDefaultValue;

    private Submission submission;

    @BeforeEach
    void setup() {
        var inputData = Map.of("testKey", "this is a test value", "otherTestKey", List.of("A", "B", "C"));
        submission = Submission.builder().inputData(inputData).urlParams(new HashMap<>()).flow("testFlow").build();
        submission = submissionRepositoryService.save(submission);
    }

    @AfterEach
    void tearDown() {
        userFileRepository.deleteAll();
        submissionRepository.deleteAll();
    }

    @Test
    void shouldUseProperDefaultForDocTypeLabel() {
        UserFile testFile = UserFile.builder().fileId(UUID.randomUUID()).submission(submission).originalName("originalName.jpg")
                .repositoryPath("/some/path/here").filesize((float) 1000.0).mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .virusScanned(true).build();

        UserFile savedFile = saveAndReload(testFile);
        assertThat(savedFile.getDocTypeLabel()).isEqualTo(docTypeDefaultValue);
    }

    @Test
    void shouldUseDocTypeSetInUserFileBuilderCall() {
        UserFile testFile = UserFile.builder().fileId(UUID.randomUUID()).submission(submission).originalName("originalName.jpg")
                .repositoryPath("/some/path/here").filesize((float) 1000.0).mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .virusScanned(true).docTypeLabel("BirthCertificate").build();

        UserFile savedFile = saveAndReload(testFile);
        assertThat(savedFile.getDocTypeLabel()).isEqualTo("BirthCertificate");
    }

    @Test
    void setVirusScannedToTrue() {
        UserFile testFile1 = UserFile.builder().fileId(UUID.randomUUID()).submission(submission).originalName("originalName.jpg")
                .repositoryPath("/some/path/here").filesize((float) 1000.0).mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .virusScanned(false).docTypeLabel("BirthCertificate").build();

        UserFile testFile2 = UserFile.builder().fileId(UUID.randomUUID()).submission(submission).originalName("originalName.jpg")
                .repositoryPath("/some/path/here").filesize((float) 1000.0).mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .virusScanned(false).docTypeLabel("BirthCertificate").build();

        UserFile testFile3 = UserFile.builder().fileId(UUID.randomUUID()).submission(submission).originalName("originalName.jpg")
                .repositoryPath("/some/path/here").filesize((float) 1000.0).mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .virusScanned(false).docTypeLabel("BirthCertificate").build();

        UserFile testFile4 = UserFile.builder().fileId(UUID.randomUUID()).submission(submission).originalName("originalName.jpg")
                .repositoryPath("/some/path/here").filesize((float) 1000.0).mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                .virusScanned(false).docTypeLabel("BirthCertificate").build();

        UserFile savedFile1 = saveAndReload(testFile1);
        UserFile savedFile2 = saveAndReload(testFile2);
        UserFile savedFile3 = saveAndReload(testFile3);
        UserFile savedFile4 = saveAndReload(testFile4);

        assertThat(savedFile1.isVirusScanned()).isEqualTo(false);
        assertThat(savedFile2.isVirusScanned()).isEqualTo(false);
        assertThat(savedFile3.isVirusScanned()).isEqualTo(false);
        assertThat(savedFile4.isVirusScanned()).isEqualTo(false);

        List<UUID> fileIds = List.of(savedFile1.getFileId(), savedFile2.getFileId(), savedFile3.getFileId());

        userFileRepositoryService.updateVirusScannedTrueByIds(fileIds);

        savedFile1 = userFileRepositoryService.findById(savedFile1.getFileId()).orElseThrow();
        savedFile2 = userFileRepositoryService.findById(savedFile2.getFileId()).orElseThrow();
        savedFile3 = userFileRepositoryService.findById(savedFile3.getFileId()).orElseThrow();
        savedFile4 = userFileRepositoryService.findById(savedFile4.getFileId()).orElseThrow();

        assertThat(savedFile1.isVirusScanned()).isEqualTo(true);
        assertThat(savedFile2.isVirusScanned()).isEqualTo(true);
        assertThat(savedFile3.isVirusScanned()).isEqualTo(true);
        assertThat(savedFile4.isVirusScanned()).isEqualTo(false);
    }

    private UserFile saveAndReload(UserFile testFile) {
        UserFile savedFile = userFileRepositoryService.save(testFile);
        return userFileRepositoryService.findById(savedFile.getFileId()).orElseThrow();
    }
}
