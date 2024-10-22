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
        "form-flow.short-code.short-code-configs.testFlow.code-length=5",
        "form-flow.short-code.short-code-configs.testFlow.code-type=alpha",
        "form-flow.short-code.short-code-configs.testFlow.suffix=-TEST"
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

        ShortCodeConfig.Config config = shortCodeConfig.getConfig(submission.getFlow());

        int expectedLength = config.getCodeLength() +
                (config.getPrefix() != null ? config.getPrefix().length() : 0) +
                (config.getSuffix() != null ? config.getSuffix().length() : 0);

        assertThat(submission.getShortCode().length()).isEqualTo(expectedLength);

        String coreOfCode = submission.getShortCode();
        if (config.getPrefix() != null) {
            coreOfCode = submission.getShortCode().substring(config.getPrefix().length());
        }

        if (config.getSuffix() != null) {
            coreOfCode = coreOfCode.substring(0, coreOfCode.length() - config.getSuffix().length());
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
