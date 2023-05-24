package formflow.library.pdf;

import formflow.library.config.ActionManager;
import formflow.library.data.Submission;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SingleFieldPreparersTest {

    private Submission testSubmission;

    @Autowired
    private ActionManager actionManager;

    private PdfMapConfiguration pdfMapConfiguration = new PdfMapConfiguration(List.of());

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
                List.of(failingPreparer, successfulPreparer), List.of(), new PdfMapConfiguration(List.of(new PdfMap())), actionManager);
        Date date = DateTime.parse("2020-09-02").toDate();

        Map<String, SubmissionField> mockOutput = Map.of(
                "submittedAt", new DatabaseField("submittedAt", String.valueOf(date))
        );
        when(successfulPreparer.prepareSubmissionFields(eq(testSubmission), anyMap(), any())).thenReturn(mockOutput);
        when(failingPreparer.prepareSubmissionFields(eq(testSubmission), anyMap(), any()))
                .thenThrow(IllegalArgumentException.class);

        List<SubmissionField> actualOutput = submissionFieldPreparers
                .prepareSubmissionFields(testSubmission);
        assertThat(actualOutput).isNotEmpty();
        // Default document fields
        assertThat(actualOutput).containsExactly(
                new DatabaseField("submittedAt", String.valueOf(testSubmission.getSubmittedAt()))
        );
        verify(successfulPreparer).prepareSubmissionFields(eq(testSubmission), anyMap(), any());
        verify(failingPreparer).prepareSubmissionFields(eq(testSubmission), anyMap(), any());
    }
}
