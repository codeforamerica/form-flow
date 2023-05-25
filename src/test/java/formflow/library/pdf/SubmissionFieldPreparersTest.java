package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.config.ActionManager;
import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmissionFieldPreparersTest {

  PdfMapConfiguration pdfMapConfiguration;
  Submission submission;

  OneToOnePreparer defaultSingleFieldPreparer = new OneToOnePreparer();
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
    pdfMapSubflow.setFields(Map.of(
        "foo", "FOO_FIELD",
        "bar", "BAR_FIELD"
    ));
    pdfMap.setSubflowInfo(Map.of("testSubflow", pdfMapSubflow));
    pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMap));
  }

  @Test
  void submissionFieldPreparersShouldCreateSubmissionFieldsForAllFieldsInAPdfMapConfigurationWhileOverwritingDefaultFieldsWithCustomFieldsThatPrepareTheSameInput() {
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(List.of(defaultSingleFieldPreparer),
        List.of(testCustomPreparer), pdfMapConfiguration, new ActionManager(List.of()));

    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten",
                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten"
            )).build();

    assertThat(defaultSingleFieldPreparer.prepareSubmissionFields(submission, submission.getInputData(),
        pdfMapConfiguration.getPdfMap("flow1"))).containsExactly(
        Map.entry("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "willBeOverwritten", null)),
        Map.entry("fieldThatDoesNotGetOverwritten", new SingleField("fieldThatGetsOverwritten", "willNotBeOverwritten", null))
    );
    assertThat(testCustomPreparer.prepareSubmissionFields(submission, submission.getInputData(),
        pdfMapConfiguration.getPdfMap("flow1"))).containsExactly(
        Map.entry("fieldThatGetsOverwritten", new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null))
    );
    assertThat(submissionFieldPreparers.prepareSubmissionFields(submission)).containsExactlyInAnyOrder(
        new SingleField("fieldThatDoesNotGetOverwritten", "willNotBeOverwritten", null),
        new SingleField("fieldThatGetsOverwritten", "OVERWRITTEN", null)
    );
  }

  @Test
  void shouldNotMapIterationsInASubflowGreaterThanMaxIterations() {
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(List.of(defaultSingleFieldPreparer),
        List.of(testCustomPreparer), pdfMapConfiguration, new ActionManager(List.of()));

    // Unfortunately can't use Map.of() here because it's immutable
    Map<String, String> iteration1 = new HashMap<>();
    iteration1.put("foo", "the foo 1");
    iteration1.put("bar", "the bar 1");
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", "true");
    Map<String, String> iteration2 = new HashMap<>();
    iteration2.put("foo", "the foo 2");
    iteration2.put("bar", "the bar 2");
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", "true");
    Map<String, String> iteration3 = new HashMap<>();
    iteration3.put("foo", "the foo 3");
    iteration3.put("bar", "the bar 3");
    iteration3.put("uuid", "uuid3");
    iteration3.put("iterationIsComplete", "true");
    Map<String, String> iteration4 = new HashMap<>();
    iteration4.put("foo", "the foo 4");
    iteration4.put("bar", "the bar 4");
    iteration4.put("uuid", "uuid4");
    iteration4.put("iterationIsComplete", "true");
    Map<String, String> iteration5 = new HashMap<>();
    iteration5.put("foo", "the foo 5");
    iteration5.put("bar", "the bar 5");
    iteration5.put("uuid", "uuid5");
    iteration5.put("iterationIsComplete", "true");
    Map<String, String> iteration6 = new HashMap<>();
    iteration6.put("foo", "the foo 6");
    iteration6.put("bar", "the bar 6");
    iteration6.put("uuid", "uuid6");
    iteration6.put("iterationIsComplete", "true");
    Map<String, String> iteration7 = new HashMap<>();
    iteration7.put("foo", "the foo 7");
    iteration7.put("bar", "the bar 7");
    iteration7.put("uuid", "uuid7");
    iteration7.put("iterationIsComplete", "true");
    Map<String, String> iteration8 = new HashMap<>();
    iteration8.put("foo", "the foo 8");
    iteration8.put("bar", "the bar 8");
    iteration8.put("uuid", "uuid8");
    iteration8.put("iterationIsComplete", "true");
    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten",
                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten",
                "testSubflow", List.of(
                    iteration1,
                    iteration2,
                    iteration3,
                    iteration4,
                    iteration5,
                    iteration6,
                    iteration7,
                    iteration8
                )
            )).build();

    assertThat(submissionFieldPreparers.prepareSubflowData(submission, pdfMapConfiguration.getPdfMap("flow1").getSubflowInfo())
        .equals(
            Map.of(
                "foo_1", "the foo 1", "bar_1", "the bar 1",
                "foo_2", "the foo 2", "bar_2", "the bar 2",
                "foo_3", "the foo 3", "bar_3", "the bar 3",
                "foo_4", "the foo 4", "bar_4", "the bar 4",
                "foo_5", "the foo 5", "bar_5", "the bar 5"
            )
        )).isTrue();
  }

  @Test
  void submissionWithLessIterationsThanMaxIterationsShouldOnlyMapExistingIterations() {
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(List.of(defaultSingleFieldPreparer),
        List.of(testCustomPreparer), pdfMapConfiguration, new ActionManager(List.of()));

    // Unfortunately can't use Map.of() here because it's immutable
    Map<String, String> iteration1 = new HashMap<>();
    iteration1.put("foo", "the foo 1");
    iteration1.put("bar", "the bar 1");
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", "true");
    Map<String, String> iteration2 = new HashMap<>();
    iteration2.put("foo", "the foo 2");
    iteration2.put("bar", "the bar 2");
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", "true");
    Map<String, String> iteration3 = new HashMap<>();
    iteration3.put("foo", "the foo 3");
    iteration3.put("bar", "the bar 3");
    iteration3.put("uuid", "uuid3");
    iteration3.put("iterationIsComplete", "true");

    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten",
                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten",
                "testSubflow", List.of(
                    iteration1,
                    iteration2,
                    iteration3
                )
            )).build();

    assertThat(submissionFieldPreparers.prepareSubflowData(submission, pdfMapConfiguration.getPdfMap("flow1").getSubflowInfo())
        .equals(
            Map.of(
                "foo_1", "the foo 1", "bar_1", "the bar 1",
                "foo_2", "the foo 2", "bar_2", "the bar 2",
                "foo_3", "the foo 3", "bar_3", "the bar 3"
            )
        )).isTrue();
  }
}