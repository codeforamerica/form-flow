package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void customPreparerFieldsShouldComeAfterDefaultPreparerFields() {
    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten"
            )).build();

    OneToOnePreparer defaultSingleFieldPreparer = new OneToOnePreparer(pdfMapConfiguration);
    TestCustomPreparer testCustomPreparer = new TestCustomPreparer();

    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(List.of(defaultSingleFieldPreparer),
        List.of(testCustomPreparer));

    assertThat(submissionFieldPreparers.prepareSubmissionFields(submission)).containsExactly(
        new SingleField("fieldThatGetsOverwritten", "willBeOverwritten", null),
        new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null)
    );
  }
}