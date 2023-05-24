package formflow.library.pdf;

import formflow.library.data.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        map.setInputFields(Map.of(
                "inputName1", "PDF_FIELD_NAME_1",
                "inputName2", "PDF_FIELD_NAME_2"
        ));
        submission.setInputData(Map.of(
                "inputName1", "foo",
                "inputName2", "bar"
        ));
        OneToOnePreparer oneToOnePreparer = new OneToOnePreparer();

        assertThat(oneToOnePreparer.prepareSubmissionFields(submission, Map.of(), new PdfMap())).containsExactly(
                Map.entry("inputName1", new SingleField("inputName1", "foo", null)),
                Map.entry("inputName2", new SingleField("inputName2", "bar", null))
        );
    }

    @Test
    void prepareIgnoresNonStringPdfMapFields() {
        map.setInputFields(Map.of(
                "inputName1", "PDF_FIELD_NAME_1",
                "inputName2", Map.of(
                        "checkboxOption1", "PDF_FIELD_NAME_3" // Ignored because it's not a string 
                )));
        submission.setInputData(Map.of(
                "inputName1", "foo",
                "inputName2", "ignoredValue"
        ));
        OneToOnePreparer oneToOnePreparer = new OneToOnePreparer();

        assertThat(oneToOnePreparer.prepareSubmissionFields(submission, Map.of(), new PdfMap())).containsExactly(
                Map.entry("inputName1", new SingleField("inputName1", "foo", null))
        );
    }
}