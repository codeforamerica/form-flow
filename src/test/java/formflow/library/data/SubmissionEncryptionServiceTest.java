package formflow.library.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class SubmissionEncryptionServiceTest {

    @Autowired
    SubmissionEncryptionService service;

    private Submission submission;

    @BeforeEach
    void setup() {
        submission = Submission.builder()
                .inputData(Map.of(
                                "checkBoxSet", List.of(),
                                "ssnInput", "123-45-6789",
                                "subflowA", List.of(Map.of("ssnInputSubflow", "321-54-9876"))
                        )
                )
                .urlParams(new HashMap<>())
                .flow("testFlow")
                .submittedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void encryptStringField() {
        Submission encryptedSubmission = service.encrypt(submission);

        assertThat(encryptedSubmission.getInputData().containsKey("ssnInput")).isFalse();
        assertThat(encryptedSubmission.getInputData().containsKey("ssnInput" + service.ENCRYPT_SUFFIX)).isTrue();
        assertThat(encryptedSubmission.getInputData().get("ssnInput" + service.ENCRYPT_SUFFIX))
                .isNotEqualTo(submission.getInputData().get("ssnInput"));
    }

    @Test
    void encryptStringFieldInSubflow() {
        Submission encryptedSubmission = service.encrypt(submission);
        var encryptedSubflow = getFirstSubflowEntry(encryptedSubmission, "subflowA");
        var originalSubflow = getFirstSubflowEntry(submission, "subflowA");

        assertThat(encryptedSubflow.containsKey("ssnInputSubflow" + service.ENCRYPT_SUFFIX)).isTrue();
        assertThat(encryptedSubflow.containsKey("ssnInputSubflow")).isFalse();
        assertThat(encryptedSubflow.get("ssnInputSubflow" + service.ENCRYPT_SUFFIX)).isNotEqualTo(
                originalSubflow.get("ssnInputSubflow"));
    }

    @Test
    void decryptStringInField() {
        Submission encryptedSubmission = service.encrypt(submission);
        Submission decryptedSubmission = service.decrypt(encryptedSubmission);

        assertThat(decryptedSubmission.getInputData().containsKey("ssnInput")).isTrue();
        assertThat(decryptedSubmission.getInputData().get("ssnInput")).isEqualTo("123-45-6789");
        assertThat(decryptedSubmission.getInputData().containsKey("ssnInput" + service.ENCRYPT_SUFFIX)).isFalse();
        assertThat(decryptedSubmission.getInputData().get("ssnInput"))
                .isNotEqualTo(encryptedSubmission.getInputData().get("ssnInput" + service.ENCRYPT_SUFFIX));
    }

    @Test
    void decryptStringFieldInSubflow() {
        Submission encryptedSubmission = service.encrypt(submission);
        Submission decryptedSubmission = service.decrypt(encryptedSubmission);

        var decryptedSubflow = getFirstSubflowEntry(decryptedSubmission, "subflowA");
        var originalSubflow = getFirstSubflowEntry(submission, "subflowA");

        assertThat(decryptedSubflow.containsKey("ssnInputSubflow")).isTrue();
        assertThat(decryptedSubflow.containsKey("ssnInputSubflow" + service.ENCRYPT_SUFFIX)).isFalse();
        assertThat(decryptedSubflow.get("ssnInputSubflow")).isEqualTo(originalSubflow.get("ssnInputSubflow"));
    }

    /**
     * Submission input data is stored as {@code Map<String, Object>}, so reading a subflow's first iteration back
     * out is an unavoidable unchecked cast - centralized here instead of repeated at every call site.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getFirstSubflowEntry(Submission submission, String subflowName) {
        return ((List<Map<String, Object>>) submission.getInputData().get(subflowName)).get(0);
    }
}
