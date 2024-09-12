package formflow.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "form-flow.path=flows-config/test-flow.yaml",
        "form-flow.short-code.length=7",
        "form-flow.short-code.type=numeric"
})
class ShortCodeConfigNumericTest {

    @Autowired
    private SubmissionRepositoryService submissionRepositoryService;

    @Test
    void testShortCodeGeneration_Numeric() {
        Submission submission = new Submission();
        submission.setFlow("testFlow");
        submission = saveAndReload(submission);

        submissionRepositoryService.generateAndSetUniqueShortCode(submission);

        assertThat(submission.getShortCode().length()).isEqualTo(7);
        assertThat(submission.getShortCode().matches("[0-9]+")).isEqualTo(true);

        Optional<Submission> reloadedSubmission = submissionRepositoryService.findByShortCode(submission.getShortCode());
        if (reloadedSubmission.isPresent()) {
            assertThat(submission).isEqualTo(reloadedSubmission.get());
            assertThat(submission.getShortCode()).isEqualTo(reloadedSubmission.get().getShortCode());
        } else {
            Assertions.fail();
        }
    }

    private Submission saveAndReload(Submission submission) {
        Submission savedSubmission = submissionRepositoryService.save(submission);
        return submissionRepositoryService.findById(savedSubmission.getId()).orElseThrow();
    }
}
