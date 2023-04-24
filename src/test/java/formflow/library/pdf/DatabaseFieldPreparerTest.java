package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.Date;
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
    submission.setSubmittedAt(DateTime.now().toDate());
    submission.setCreatedAt(DateTime.now().minusHours(1).toDate());
    submission.setId(UUID.randomUUID());
    submission.setUpdatedAt(DateTime.now().toDate());
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
    Date date = DateTime.now().toDate();
    DataBaseFieldPreparer dataBaseFieldPreparer = new DataBaseFieldPreparer(pdfMapConfiguration);
    assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission)).contains(
        new DatabaseField("submittedAt", date.toString())
    );
  }

  @Test
  void shouldNotCreateDbFieldsForItemsNotPresentInPdfMap() {
    DataBaseFieldPreparer dataBaseFieldPreparer = new DataBaseFieldPreparer(pdfMapConfiguration);
    assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission)).containsExactlyInAnyOrder(
        new DatabaseField("flow", submission.getFlow()),
        new DatabaseField("submittedAt", submission.getSubmittedAt().toString()),
        new DatabaseField("createdAt", submission.getCreatedAt().toString())
    );

    assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission)).doesNotContain(
        new DatabaseField("submissionId", submission.getId().toString()),
        new DatabaseField("updatedAt", submission.getUpdatedAt().toString())
    );
  }
}