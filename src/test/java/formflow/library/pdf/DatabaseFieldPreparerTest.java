package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import formflow.library.data.Submission;
import formflow.library.utilities.TestUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseFieldPreparerTest {

    PdfMapConfiguration pdfMapConfiguration;
    Submission submission;

    @BeforeEach
    void setUp() {
        submission = Submission.builder().flow("flow1").build();
        submission.setSubmittedAt(TestUtils.makeOffsetDateTime("2020-09-15"));
        submission.setCreatedAt(TestUtils.makeOffsetDateTime("2020-09-15").minusDays(1));
        submission.setId(UUID.randomUUID());
        submission.setUpdatedAt(TestUtils.makeOffsetDateTime("2020-09-15"));
        PdfMap pdfMap = new PdfMap();
        pdfMap.setDbFields(Map.of(
                        "submittedAt", "SUBMITTED_AT",
                        "flow", "FLOW",
                        "createdAt", "CREATED_AT"
                )
        );

        pdfMap.setFlow("flow1");
        pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMap));
    }

    @Test
    void prepareReturnsDatabaseFieldsSubmittedAtDate() {
        DatabaseFieldPreparer dataBaseFieldPreparer = new DatabaseFieldPreparer();
        assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow()))).containsEntry(
                "submittedAt", new DatabaseField("submittedAt", "09/15/2020")
        );
    }

    @Test
    void shouldNotCreateDbFieldsForItemsNotPresentInPdfMap() {
        DatabaseFieldPreparer dataBaseFieldPreparer = new DatabaseFieldPreparer();
        assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow()))).containsAllEntriesOf(
                Map.of("flow", new DatabaseField("flow", submission.getFlow()),
                        "submittedAt", new DatabaseField("submittedAt", "09/15/2020"),
                        "createdAt", new DatabaseField("createdAt", "09/14/2020"))
        );

        assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow()))).doesNotContain(
                Map.entry("submissionId", new DatabaseField("submissionId", submission.getId().toString())),
                Map.entry("updatedAt", new DatabaseField("updatedAt", "09/15/2020"))
        );
    }

    @Test
    void DbFieldsShouldNotBeRequired_shouldNotThrowErrorIfDbFieldsIsNull() {
        DatabaseFieldPreparer dataBaseFieldPreparer = new DatabaseFieldPreparer();
        PdfMap pdfMap = new PdfMap();
        pdfMap.setFlow("flow1");
        // Assert Does Not throw for a completely missing dbFields Map
        pdfMapConfiguration = new PdfMapConfiguration(List.of(pdfMap));
        assertDoesNotThrow(() -> dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow())));
        assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow()))).isEmpty();

        // Assert does not throw for a null dbFields Map value
        pdfMap.setDbFields(null);
        assertDoesNotThrow(() -> dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow())));
        assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow()))).isEmpty();

        // Assert does not throw for an empty dbFields Map value
        pdfMap.setDbFields(Map.of());
        assertDoesNotThrow(() -> dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow())));
        assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission,
                pdfMapConfiguration.getPdfMap(submission.getFlow()))).isEmpty();
    }
}
