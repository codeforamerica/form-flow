package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleFieldPreparersTest {

  private Submission testSubmission;

  @BeforeEach
  void setUp() {
    testSubmission = Submission.builder()
        .id(UUID.randomUUID())
        .submittedAt(DateTime.parse("2020-09-02").toDate())
        .build();
  }

  @Test
  void shouldStillSuccessfullyMapEvenWithExceptionsInIndividualPreparers() {
    DefaultSubmissionFieldPreparer successfulPreparer = mock(DefaultSubmissionFieldPreparer.class);
    DefaultSubmissionFieldPreparer failingPreparer = mock(DefaultSubmissionFieldPreparer.class);
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(
        List.of(failingPreparer, successfulPreparer), List.of());
    Date date = DateTime.parse("2020-09-02").toDate();

    Map<String, SubmissionField> mockOutput = Map.of(
        "submittedAt", new DatabaseField("submittedAt", String.valueOf(date))
    );
    when(successfulPreparer.prepareSubmissionFields(eq(testSubmission))).thenReturn(mockOutput);
    when(failingPreparer.prepareSubmissionFields(eq(testSubmission)))
        .thenThrow(IllegalArgumentException.class);

    List<SubmissionField> actualOutput = submissionFieldPreparers
        .prepareSubmissionFields(testSubmission);
    assertThat(actualOutput).isNotEmpty();
    // Default document fields
    assertThat(actualOutput).containsExactly(
        new DatabaseField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()))
    );
    verify(successfulPreparer).prepareSubmissionFields(eq(testSubmission));
    verify(failingPreparer).prepareSubmissionFields(eq(testSubmission));
  }
}
