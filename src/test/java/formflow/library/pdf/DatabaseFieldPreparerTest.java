package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.Date;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseFieldPreparerTest {

  Submission submission;

  @BeforeEach
  void setUp() {
    submission = Submission.builder().flow("flow1").build();
  }

  @Test
  void prepareReturnsDatabaseFieldsSubmittedAtDate() {
    Date date = DateTime.now().toDate();
    submission.setSubmittedAt(date);
    DataBaseFieldPreparer dataBaseFieldPreparer = new DataBaseFieldPreparer();

    assertThat(dataBaseFieldPreparer.prepareSubmissionFields(submission)).containsExactlyInAnyOrder(
        new DatabaseField("submittedAt", date.toString())
    );
  }
}