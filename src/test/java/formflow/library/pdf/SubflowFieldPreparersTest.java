package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubflowFieldPreparersTest {

  PdfMapConfiguration pdfMapConfiguration;
  Submission submission;

  SubflowFieldPreparer subflowFieldPreparer;


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
        "bar", "BAR_FIELD",
        "checkboxInput", Map.of("item1", "PDF_ITEM1", "item2", "PDF_ITEM2", "item3", "PDF_ITEM3")
    ));
    pdfMap.setSubflowInfo(Map.of("testSubflow", pdfMapSubflow));
    pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMap));

    subflowFieldPreparer = new SubflowFieldPreparer();
  }

  @Test
  void shouldNotMapIterationsInASubflowGreaterThanMaxIterations() {

    // Unfortunately can't use Map.of() here because it's immutable
    Map<String, Object> iteration1 = new HashMap<>();
    iteration1.put("foo", "the foo 1");
    iteration1.put("bar", "the bar 1");
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", true);
    Map<String, Object> iteration2 = new HashMap<>();
    iteration2.put("foo", "the foo 2");
    iteration2.put("bar", "the bar 2");
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", true);
    Map<String, Object> iteration3 = new HashMap<>();
    iteration3.put("foo", "the foo 3");
    iteration3.put("bar", "the bar 3");
    iteration3.put("uuid", "uuid3");
    iteration3.put("iterationIsComplete", true);
    Map<String, Object> iteration4 = new HashMap<>();
    iteration4.put("foo", "the foo 4");
    iteration4.put("bar", "the bar 4");
    iteration4.put("uuid", "uuid4");
    iteration4.put("iterationIsComplete", true);
    Map<String, Object> iteration5 = new HashMap<>();
    iteration5.put("foo", "the foo 5");
    iteration5.put("bar", "the bar 5");
    iteration5.put("uuid", "uuid5");
    iteration5.put("iterationIsComplete", true);
    Map<String, Object> iteration6 = new HashMap<>();
    iteration6.put("foo", "the foo 6");
    iteration6.put("bar", "the bar 6");
    iteration6.put("uuid", "uuid6");
    iteration6.put("iterationIsComplete", true);
    Map<String, Object> iteration7 = new HashMap<>();
    iteration7.put("foo", "the foo 7");
    iteration7.put("bar", "the bar 7");
    iteration7.put("uuid", "uuid7");
    iteration7.put("iterationIsComplete", true);
    Map<String, Object> iteration8 = new HashMap<>();
    iteration8.put("foo", "the foo 8");
    iteration8.put("bar", "the bar 8");
    iteration8.put("uuid", "uuid8");
    iteration8.put("iterationIsComplete", true);
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

    Map<String, SubmissionField> resultMap = subflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"));

    assertThat(resultMap.equals(
        Map.of(
            "foo_1", new SingleField("foo", "the foo 1", 1),
            "bar_1", new SingleField("bar", "the bar 1", 1),
            "foo_2", new SingleField("foo", "the foo 2", 2),
            "bar_2", new SingleField("bar", "the bar 2", 2),
            "foo_3", new SingleField("foo", "the foo 3", 3),
            "bar_3", new SingleField("bar", "the bar 3", 3),
            "foo_4", new SingleField("foo", "the foo 4", 4),
            "bar_4", new SingleField("bar", "the bar 4", 4),
            "foo_5", new SingleField("foo", "the foo 5", 5),
            "bar_5", new SingleField("bar", "the bar 5", 5)
        )
    )).isTrue();
  }

  @Test
  void submissionWithLessIterationsThanMaxIterationsShouldOnlyMapExistingIterations() {

    // Unfortunately can't use Map.of() here because it's immutable
    Map<String, Object> iteration1 = new HashMap<>();
    iteration1.put("foo", "the foo 1");
    iteration1.put("bar", "the bar 1");
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", true);
    Map<String, Object> iteration2 = new HashMap<>();
    iteration2.put("foo", "the foo 2");
    iteration2.put("bar", "the bar 2");
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", true);
    Map<String, Object> iteration3 = new HashMap<>();
    iteration3.put("foo", "the foo 3");
    iteration3.put("bar", "the bar 3");
    iteration3.put("uuid", "uuid3");
    iteration3.put("iterationIsComplete", true);

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

    Map<String, SubmissionField> resultMap = subflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"));

    assertThat(resultMap.equals(
        Map.of(
            "foo_1", new SingleField("foo", "the foo 1", 1),
            "bar_1", new SingleField("bar", "the bar 1", 1),
            "foo_2", new SingleField("foo", "the foo 2", 2),
            "bar_2", new SingleField("bar", "the bar 2", 2),
            "foo_3", new SingleField("foo", "the foo 3", 3),
            "bar_3", new SingleField("bar", "the bar 3", 3)
        )
    )).isTrue();
  }

  @Test
  void shouldAddCorrectSuffixForCheckboxFieldsInSubflows() {

    Map<String, Object> iteration1 = new HashMap<>();
    iteration1.put("foo", "the foo 1");
    iteration1.put("bar", "the bar 1");
    iteration1.put("checkboxInput[]", List.of("item1", "item2", "item3"));
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", true);
    Map<String, Object> iteration2 = new HashMap<>();
    iteration2.put("foo", "the foo 2");
    iteration2.put("bar", "the bar 2");
    iteration2.put("checkboxInput[]", List.of("item1", "item2", "item3"));
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", true);

    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten",
                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten",
                "testSubflow", List.of(
                    iteration1,
                    iteration2
                )
            )).build();

    Map<String, SubmissionField> resultMap = subflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"));

    Map<String, SubmissionField> expectedMap =
        Map.of(
            "foo_1", new SingleField("foo", "the foo 1", 1),
            "bar_1", new SingleField("bar", "the bar 1", 1),
            "checkboxInput_1[]", new CheckboxField("checkboxInput", List.of("item1", "item2", "item3"), 1),
            "foo_2", new SingleField("foo", "the foo 2", 2),
            "bar_2", new SingleField("bar", "the bar 2", 2),
            "checkboxInput_2[]", new CheckboxField("checkboxInput", List.of("item1", "item2", "item3"), 2)
        );

    assertThat(resultMap.keySet().equals(expectedMap.keySet())).isTrue();

    assertThat(resultMap.equals(expectedMap)).isTrue();
  }

  @Test
  void shouldNotThrownWhenNoSubflow() {
    PdfMap pdfMapWithoutSubflow = new PdfMap();
    pdfMapWithoutSubflow.setInputFields(Map.of(
            "fieldThatGetsOverwritten", "TEST_FIELD",
            "fieldThatDoesNotGetOverwritten", "TEST_FIELD_2"
        )
    );
    pdfMapWithoutSubflow.setFlow("flow1");
    pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMapWithoutSubflow));

    SubflowFieldPreparer noSubflowFieldPreparer = new SubflowFieldPreparer();

    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of(
                "fieldThatGetsOverwritten", "willBeOverwritten",
                "fieldThatDoesNotGetOverwritten", "willNotBeOverwritten"
            )).build();

    noSubflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"));

    assertThatCode(() -> noSubflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"))
    ).doesNotThrowAnyException();
  }
  
  @Test
  void shouldNotCreateFieldsForIncompleteSubflowIterations() {
    Map<String, Object> iteration1 = new HashMap<>();
    iteration1.put("foo", "foo from first iteration will not be removed because iteration is complete");
    iteration1.put("bar", "bar from first iteration will not be removed because iteration is complete");
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", true);
    Map<String, Object> iteration2 = new HashMap<>();
    iteration2.put("foo", "foo from second iteration will be removed because iteration is not complete");
    iteration2.put("bar", "bar from second iteration will be removed because iteration is not complete");
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", false);
    Map<String, Object> iteration3 = new HashMap<>();
    iteration3.put("foo", "foo from third iteration will not be removed because iteration is complete");
    iteration3.put("bar", "bar from third iteration will not be removed because iteration is complete");
    iteration3.put("uuid", "uuid3");
    iteration3.put("iterationIsComplete", true);

    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of("testSubflow", List.of(
                iteration1,
                iteration2,
                iteration3)
            )).build();

    Map<String, SubmissionField> resultMap = subflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"));

    assertThat(resultMap.equals(
        Map.of(
            "foo_1", new SingleField("foo", "foo from first iteration will not be removed because iteration is complete", 1),
            "bar_1", new SingleField("bar", "bar from first iteration will not be removed because iteration is complete", 1),
            "foo_2", new SingleField("foo", "foo from third iteration will not be removed because iteration is complete", 2),
            "bar_2", new SingleField("bar", "bar from third iteration will not be removed because iteration is complete", 2)
        )
    )).isTrue();
  }
  
  @Test
  void shouldNotPrepareIgnoredSubflowFields() {
    Map<String, Object> iteration1 = new HashMap<>();
    iteration1.put("foo", "foo 1");
    iteration1.put("bar", "bar 1");
    iteration1.put("uuid", "uuid1");
    iteration1.put("iterationIsComplete", true);
    Map<String, Object> iteration2 = new HashMap<>();
    iteration2.put("foo", "foo 2");
    iteration2.put("bar", "bar 2");
    iteration2.put("uuid", "uuid2");
    iteration2.put("iterationIsComplete", true);


    submission = Submission.builder().flow("flow1")
        .inputData(
            Map.of("testSubflow", List.of(
                iteration1,
                iteration2)
            )).build();

    Map<String, SubmissionField> resultMap = subflowFieldPreparer.prepareSubmissionFields(submission,
        pdfMapConfiguration.getPdfMap("flow1"));
    
    
    // Result does not include UUID or iterationIsComplete which should be ignored
    assertThat(resultMap.equals(
        Map.of(
            "foo_1", new SingleField("foo", "foo 1", 1),
            "bar_1", new SingleField("bar", "bar 1", 1),
            "foo_2", new SingleField("foo", "foo 2", 2),
            "bar_2", new SingleField("bar", "bar 2", 2)
        )
    )).isTrue();
  }
}
