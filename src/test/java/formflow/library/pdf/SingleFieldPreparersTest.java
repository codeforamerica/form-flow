package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleFieldPreparersTest {

  private SubmissionFieldPreparers preparers;

  private Submission testSubmission;

  @BeforeEach
  void setUp() {
    preparers = new SubmissionFieldPreparers(List.of());
    testSubmission = Submission.builder()
        .id(UUID.randomUUID())
        .submittedAt(DateTime.parse("2020-09-02").toDate())
        .build();
  }

  @Test
  void shouldIncludeSubmissionIdInput() {
    List<SubmissionField> singleFields = preparers.prepareSubmissionFields(testSubmission);

    assertThat(singleFields).contains(
        new SingleField("submissionId", String.valueOf(testSubmission.getId()), null));
  }

  @Test
  void shouldIncludeSubmittedAtTime() {
    List<SubmissionField> singleFields = preparers.prepareSubmissionFields(testSubmission);

    assertThat(singleFields).contains(
        new SingleField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()), null));
  }


  @Test
  void shouldStillSuccessfullyMapEvenWithExceptionsInIndividualPreparers() {
    SubmissionFieldPreparer successfulPreparer = mock(SubmissionFieldPreparer.class);
    SubmissionFieldPreparer failingPreparer = mock(SubmissionFieldPreparer.class);
    SubmissionFieldPreparers submissionFieldPreparers = new SubmissionFieldPreparers(
        List.of(failingPreparer, successfulPreparer));
    Date date = DateTime.parse("2020-09-02").toDate();

    List<SubmissionField> mockOutput = List.of();
    when(successfulPreparer.prepareSubmissionFields(eq(testSubmission)))
        .thenReturn(mockOutput);
    when(failingPreparer.prepareSubmissionFields(eq(testSubmission)))
        .thenThrow(IllegalArgumentException.class);

    List<SubmissionField> actualOutput = submissionFieldPreparers
        .prepareSubmissionFields(testSubmission);
    assertThat(actualOutput).isNotEmpty();
    // Default document fields
    assertThat(actualOutput).containsExactly(
        new SingleField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()), null),
        new SingleField("submissionId", String.valueOf(testSubmission.getId()), null)
    );
    verify(successfulPreparer).prepareSubmissionFields(eq(testSubmission));
    verify(failingPreparer).prepareSubmissionFields(eq(testSubmission));
  }

  //  TODO: Add tests for single/multivalued fields
  @Test
  void prepareDocumentFieldsWorksWithMultipleValues() {

  }
}
