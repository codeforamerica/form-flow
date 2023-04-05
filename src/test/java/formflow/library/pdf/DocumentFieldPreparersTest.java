package formflow.library.pdf;

import static formflow.library.pdf.DocumentFieldType.SINGLE_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import java.util.Date;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentFieldPreparersTest {

  private DocumentFieldPreparers preparers;

  @BeforeEach
  void setUp() {
    // TODO Setup SUbmission object here
    preparers = new DocumentFieldPreparers(List.of());
  }

  @Test
  void shouldIncludeSubmissionIdInput() {
    Submission testSubmission = Submission.builder()
        .id(1L)
        .build();
    List<DocumentField> documentFields = preparers.prepareDocumentFields(testSubmission);

    assertThat(documentFields).contains(
        new DocumentField("submissionId", String.valueOf(testSubmission.getId()), SINGLE_VALUE, null));
  }

  @Test
  void shouldIncludeSubmittedAtTime() {
    Date date = DateTime.parse("2020-09-02").toDate();
    Submission submission = Submission.builder()
        .id(1L)
        .submittedAt(date)
        .build();

    List<DocumentField> documentFields = preparers.prepareDocumentFields(submission);

    assertThat(documentFields).contains(
        new DocumentField("submittedAt", String.valueOf(submission.getSubmittedAt()), SINGLE_VALUE, null));
  }

  @Test(llegalArgumentException.class)
  void shouldStillSuccessfullyMapEvenWithExceptionsInIndividualPreparers() {
    DocumentFieldPreparer successfulPreparer = mock(DocumentFieldPreparer.class);
    DocumentFieldPreparer failingPreparer = mock(DocumentFieldPreparer.class);
    DocumentFieldPreparers documentFieldPreparers = new DocumentFieldPreparers(
        List.of(failingPreparer, successfulPreparer));
    Date date = DateTime.parse("2020-09-02").toDate();
    Submission submission = Submission.builder()
        .id(1L)
        .submittedAt(date)
        .build();

    List<DocumentField> mockOutput = List.of();
    when(successfulPreparer.prepareDocumentFields(eq(submission)))
        .thenReturn(mockOutput);
    when(failingPreparer.prepareDocumentFields(eq(submission)))
        .thenThrow(IllegalArgumentException.class);

    List<DocumentField> actualOutput = documentFieldPreparers
        .prepareDocumentFields(submission);
    assertThat(actualOutput).isNotEmpty();
    verify(successfulPreparer).prepareDocumentFields(eq(submission));
    verify(failingPreparer).prepareDocumentFields(eq(submission));
  }
}
