package formflow.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.config.submission.ShortCodeConfig;
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
        "form-flow.short-code.length=5",
        "form-flow.short-code.type=alpha",
        "form-flow.short-code.suffix=-TEST"
})

class ShortCodeConfigAlphaTest {

    @Autowired
    private SubmissionRepositoryService submissionRepositoryService;

    @Autowired
    private ShortCodeConfig shortCodeConfig;

    @Test
    void testShortCodeGeneration_Numeric() {
        Submission submission = new Submission();
        submission.setFlow("testFlow");
        submission = saveAndReload(submission);

        submissionRepositoryService.generateAndSetUniqueShortCode(submission);

        int expectedLength = shortCodeConfig.getCodeLength() +
                (shortCodeConfig.getPrefix() != null ? shortCodeConfig.getPrefix().length() : 0) +
                (shortCodeConfig.getSuffix() != null ? shortCodeConfig.getSuffix().length() : 0);

        assertThat(submission.getShortCode().length()).isEqualTo(expectedLength);

        String coreOfCode = submission.getShortCode();
        if (shortCodeConfig.getPrefix() != null) {
            coreOfCode = submission.getShortCode().substring(shortCodeConfig.getPrefix().length());
        }

        if (shortCodeConfig.getSuffix() != null) {
            coreOfCode = coreOfCode.substring(0, coreOfCode.length() - shortCodeConfig.getSuffix().length());
        }

        assertThat(coreOfCode.matches("[A-Za-z]+")).isEqualTo(true);

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
