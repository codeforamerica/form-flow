package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OneToOnePreparerTest {

    PdfMap pdfMap;
    Submission submission;

    @BeforeEach
    void setUp() {
        pdfMap = new PdfMap();
        pdfMap.setFlow("flow1");
        submission = Submission.builder().flow("flow1").build();
    }

    @Test
    void prepareReturnsDocumentFieldsForSingleValues() {
        pdfMap.setInputFields(Map.of(
                "inputName1", "PDF_FIELD_NAME_1",
                "inputName2", "PDF_FIELD_NAME_2"
        ));
        submission.setInputData(Map.of(
                "inputName1", "foo",
                "inputName2", "bar"
        ));
        OneToOnePreparer oneToOnePreparer = new OneToOnePreparer();

        assertThat(oneToOnePreparer.prepareSubmissionFields(submission, pdfMap)).containsExactly(
                Map.entry("inputName1", new SingleField("inputName1", "foo", null)),
                Map.entry("inputName2", new SingleField("inputName2", "bar", null))
        );
    }

    @Test
    void prepareIgnoresNonStringPdfMapFields() {
        pdfMap.setInputFields(Map.of(
                "inputName1", "PDF_FIELD_NAME_1",
                "inputName2", Map.of(
                        "checkboxOption1", "PDF_FIELD_NAME_3" // Ignored because it's not a string
                )));
        submission.setInputData(Map.of(
                "inputName1", "foo",
                "inputName2", "ignoredValue"
        ));
        OneToOnePreparer oneToOnePreparer = new OneToOnePreparer();

        assertThat(oneToOnePreparer.prepareSubmissionFields(submission, pdfMap)).containsExactly(
                Map.entry("inputName1", new SingleField("inputName1", "foo", null))
        );
    }
}