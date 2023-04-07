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

  private Submission testSubmission;

  @BeforeEach
  void setUp() {
    // TODO Setup SUbmission object here
    preparers = new DocumentFieldPreparers(List.of());
    testSubmission = Submission.builder()
        .id(1L)
        .submittedAt(DateTime.parse("2020-09-02").toDate())
        .build();
  }

  @Test
  void shouldIncludeSubmissionIdInput() {
    List<DocumentField> documentFields = preparers.prepareDocumentFields(testSubmission);

    assertThat(documentFields).contains(
        new DocumentField("submissionId", String.valueOf(testSubmission.getId()), SINGLE_VALUE, null));
  }

  @Test
  void shouldIncludeSubmittedAtTime() {
    List<DocumentField> documentFields = preparers.prepareDocumentFields(testSubmission);

    assertThat(documentFields).contains(
        new DocumentField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()), SINGLE_VALUE, null));
  }


  //how does this test verify the documents were successfully submitted?
  @Test
  void shouldStillSuccessfullyMapEvenWithExceptionsInIndividualPreparers() {
    DocumentFieldPreparer successfulPreparer = mock(DocumentFieldPreparer.class);
    DocumentFieldPreparer failingPreparer = mock(DocumentFieldPreparer.class);
    DocumentFieldPreparers documentFieldPreparers = new DocumentFieldPreparers(
        List.of(failingPreparer, successfulPreparer));
    Date date = DateTime.parse("2020-09-02").toDate();

    List<DocumentField> mockOutput = List.of();
    when(successfulPreparer.prepareDocumentFields(eq(testSubmission)))
        .thenReturn(mockOutput);
    when(failingPreparer.prepareDocumentFields(eq(testSubmission)))
        .thenThrow(IllegalArgumentException.class);

    List<DocumentField> actualOutput = documentFieldPreparers
        .prepareDocumentFields(testSubmission);
    assertThat(actualOutput).isNotEmpty();
    // Default document fields
    assertThat(actualOutput).containsExactly(
        new DocumentField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()), SINGLE_VALUE, null),
        new DocumentField("submissionId", String.valueOf(testSubmission.getId()), SINGLE_VALUE, null)
    );
    verify(successfulPreparer).prepareDocumentFields(eq(testSubmission));
    verify(failingPreparer).prepareDocumentFields(eq(testSubmission));
  }


}
