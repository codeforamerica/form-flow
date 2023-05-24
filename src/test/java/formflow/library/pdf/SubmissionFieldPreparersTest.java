package formflow.library.pdf;

import formflow.library.config.ActionManager;
import formflow.library.data.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionFieldPreparersTest {

    PdfMapConfiguration pdfMapConfiguration;
    Submission submission;

    @BeforeEach
    void setUp() {
        PdfMap pdfMap = new PdfMap();
        pdfMap.setInputFields(Map.of(
                        "fieldThatGetsOverwritten", "TEST_FIELD",
                        "fieldThatDoesNotGetOverwritten", "TEST_FIELD_2"
                )
        );

        pdfMap.setFlow("flow1");
        pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMap));
    }

    @Test
    void submissionFieldPreparersShouldCreateSubmissionFieldsForAllFieldsInAPdfMapConfigurationWhileOverwritingDefaultFieldsWithCustomFieldsThatPrepareTheSameInput() {
        submission = Submission.builder().flow("flow1")
                .inputData(
                        Map.of(
                                "fieldThatGetsOverwritten", "willBeOverwritten",
                                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten"
                        )).build();

        OneToOnePreparer defaultSingleFieldPreparer = new OneToOnePreparer();
        TestCustomPreparer testCustomPreparer = new TestCustomPreparer();

        SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(List.of(defaultSingleFieldPreparer),
                List.of(testCustomPreparer), new PdfMapConfiguration(List.of(new PdfMap())), new ActionManager(List.of()));

        assertThat(defaultSingleFieldPreparer.prepareSubmissionFields(submission, Map.of(), new PdfMap())).containsExactly(
                Map.entry("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "willBeOverwritten", null)),
                Map.entry("fieldThatDoesNotGetOverwritten", new SingleField("fieldThatGetsOverwritten", "willNotBeOverwritten", null))
        );
        assertThat(testCustomPreparer.prepareSubmissionFields(submission, Map.of(), new PdfMap())).containsExactly(
                Map.entry("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null))
        );
        assertThat(submissionFieldPreparers.prepareSubmissionFields(submission)).containsExactlyInAnyOrder(
                new SingleField("fieldThatDoesNotGetOverwritten", "willNotBeOverwritten", null),
                new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null)
        );
    }
}