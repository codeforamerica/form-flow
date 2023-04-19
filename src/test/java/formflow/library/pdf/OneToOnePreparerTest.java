package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OneToOnePreparerTest {

  PdfMap map;
  Submission submission;

  @BeforeEach
  void setUp() {
    map = new PdfMap();
    map.setFlow("flow1");
    submission = Submission.builder().flow("flow1").build();
  }

  @Test
  void prepareReturnsDocumentFieldsForSingleValues() {
    map.setInputs(Map.of(
        "inputName1", "PDF_FIELD_NAME_1",
        "inputName2", "PDF_FIELD_NAME_2"
    ));
    submission.setInputData(Map.of(
        "inputName1", "foo",
        "inputName2", "bar"
    ));
    OneToOnePreparer oneToOnePreparer = new OneToOnePreparer(new PdfMapConfiguration(List.of(map)));

    assertThat(oneToOnePreparer.prepareDocumentFields(submission)).containsExactlyInAnyOrder(
        new SubmissionField("inputName1", "foo", SubmissionFieldValue.SINGLE_FIELD, null),
        new SubmissionField("inputName2", "bar", SubmissionFieldValue.SINGLE_FIELD, null)
    );
  }

  @Test
  void prepareIgnoresNonStringPdfMapFields() {
    map.setInputs(Map.of(
        "inputName1", "PDF_FIELD_NAME_1",
        "inputName2", Map.of(
            "checkboxOption1", "PDF_FIELD_NAME_3" // Ignored because it's not a string 
        )));
    submission.setInputData(Map.of(
        "inputName1", "foo",
        "inputName2", "ignoredValue"
    ));
    OneToOnePreparer oneToOnePreparer = new OneToOnePreparer(new PdfMapConfiguration(List.of(map)));

    assertThat(oneToOnePreparer.prepareDocumentFields(submission)).containsExactly(
        new SubmissionField("inputName1", "foo", SubmissionFieldValue.SINGLE_FIELD, null)
    );
  }
}