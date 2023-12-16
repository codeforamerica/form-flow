package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import formflow.library.utilities.TestUtils;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SingleFieldPreparersTest {

  private Submission testSubmission;

  @BeforeEach
  void setUp() {
    testSubmission = Submission.builder()
        .id(UUID.randomUUID())
        .flow("testFlow1")
        .inputData(
            Map.of("applicantName", "Mack",
                "applicantDateOfBirth", "05/01/1980"
            )
        )
        .submittedAt(TestUtils.makeOffsetDateTime("2020-09-02"))
        .build();
  }

  @Test
  @Disabled
  void shouldStillSuccessfullyMapEvenWithExceptionsInIndividualPreparers() {
    DefaultSubmissionFieldPreparer successfulPreparer = mock(DefaultSubmissionFieldPreparer.class);
    DefaultSubmissionFieldPreparer failingPreparer = mock(DefaultSubmissionFieldPreparer.class);
    PdfMapConfiguration pdfMapConfiguration = mock(PdfMapConfiguration.class);
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(
        List.of(failingPreparer, successfulPreparer), List.of(), pdfMapConfiguration);
    Date date = DateTime.parse("2020-09-02").toDate();
    PdfMap pdfMap = new PdfMap();

    Map<String, SubmissionField> mockOutput = Map.of(
        "submittedAt", new DatabaseField("submittedAt", String.valueOf(date))
    );
    when(successfulPreparer.prepareSubmissionFields(eq(testSubmission), any())).thenReturn(mockOutput);
    when(failingPreparer.prepareSubmissionFields(eq(testSubmission), any())).thenThrow(IllegalArgumentException.class);
    when(pdfMapConfiguration.getPdfMap("testFlow1")).thenReturn(pdfMap);

    List<SubmissionField> actualOutput = submissionFieldPreparers.prepareSubmissionFields(testSubmission);
    assertThat(actualOutput).isNotEmpty();
    // Default document fields
    assertThat(actualOutput).containsExactly(
        new DatabaseField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()))
    );
    verify(successfulPreparer).prepareSubmissionFields(eq(testSubmission), any());
    verify(failingPreparer).prepareSubmissionFields(eq(testSubmission), any());
  }
}
