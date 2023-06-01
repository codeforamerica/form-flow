package formflow.library.pdf;

import formflow.library.data.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OneToManyPreparerTest {

  PdfMap pdfMap;
  Submission submission;

  @BeforeEach
  void setUp() {
    pdfMap = new PdfMap();
    pdfMap.setFlow("flow1");
    submission = Submission.builder().flow("flow1").build();
  }

  @Test
  void preparesSubmissionFieldsForCheckboxInputs() {
    pdfMap.setInputFields(Map.of(
        "checkbox", Map.of(
            "option1", "CHECKBOX_OPTION_1",
            "option2", "CHECKBOX_OPTION_2",
            "option3", "CHECKBOX_OPTION_3"
        )));
    submission.setInputData(Map.of(
        "checkbox[]", List.of("option1", "option3")
    ));
    OneToManyPreparer oneToManyPreparer = new OneToManyPreparer();

    assertThat(oneToManyPreparer.prepareSubmissionFields(submission, submission.getInputData(), pdfMap)).containsExactly(
        Map.entry("checkbox", new CheckboxField("checkbox", List.of("option1", "option3"), null))
    );
  }
}