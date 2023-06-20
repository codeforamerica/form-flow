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

  OneToOnePreparer defaultSingleFieldPreparer = new OneToOnePreparer();

  OneToManyPreparer defaultCheckboxFieldPreparer = new OneToManyPreparer();

  DatabaseFieldPreparer defaultDatabaseFieldPreparer = new DatabaseFieldPreparer();

  SubflowFieldPreparer defaultSubflowFieldPreparer = new SubflowFieldPreparer();
  TestCustomPreparer testCustomPreparer = new TestCustomPreparer();

  @BeforeEach
  void setUp() {
    PdfMap pdfMap = new PdfMap();
    pdfMap.setInputFields(Map.of(
            "fieldThatGetsOverwritten", "TEST_FIELD",
            "fieldThatDoesNotGetOverwritten", "TEST_FIELD_2"
        )
    );

    pdfMap.setFlow("flow1");
    PdfMapSubflow pdfMapSubflow = new PdfMapSubflow();
    pdfMapSubflow.setSubflows(List.of("testSubflow"));
    pdfMapSubflow.setTotalIterations(5);
    pdfMapSubflow.setInputFields(Map.of(
        "foo", "FOO_FIELD",
        "bar", "BAR_FIELD"
    ));
    pdfMap.setSubflowInfo(Map.of("testSubflow", pdfMapSubflow));
    pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMap));
  }

  @Test
  void submissionFieldPreparersShouldCreateSubmissionFieldsForAllFieldsInAPdfMapConfigurationWhileOverwritingDefaultFieldsWithCustomFieldsThatPrepareTheSameInput() {
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(
        List.of(defaultSingleFieldPreparer, defaultCheckboxFieldPreparer, defaultDatabaseFieldPreparer,
            defaultSubflowFieldPreparer),
        List.of(testCustomPreparer), pdfMapConfiguration);

    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten",
                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten"
            )).build();

    assertThat(defaultSingleFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"))).containsOnly(
        Map.entry("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "willBeOverwritten", null)),
        Map.entry("fieldThatDoesNotGetOverwritten",
            new SingleField("fieldThatDoesNotGetOverwritten", "willNotBeOverwritten", null))
    );
    assertThat(testCustomPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"))).containsOnly(
        Map.entry("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null))
    );
    assertThat(submissionFieldPreparers.prepareSubmissionFields(submission)).containsOnly(
        new SingleField("fieldThatDoesNotGetOverwritten", "willNotBeOverwritten", null),
        new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null)
    );
  }
}
