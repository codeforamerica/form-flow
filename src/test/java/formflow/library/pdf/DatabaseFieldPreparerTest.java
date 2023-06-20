package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseFieldPreparerTest {

  PdfMapConfiguration pdfMapConfiguration;
  Submission submission;

  @BeforeEach
  void setUp() {
    submission = Submission.builder().flow("flow1").build();
    submission.setSubmittedAt(DateTime.parse("2020-09-15").toDate());
    submission.setCreatedAt(DateTime.parse("2020-09-15").minusDays(1).toDate());
    submission.setId(UUID.randomUUID());
    submission.setUpdatedAt(DateTime.parse("2020-09-15").toDate());
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
}