package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(properties = {"form-flow.path=flows-config/test-inputs.yaml"})
class SubmissionRepositoryServiceTest {

  @Autowired
  private SubmissionRepositoryService submissionRepositoryService;

  @Test
  void shouldSaveSubmissionsWithSequentialIds() {
    Submission firstSubmission = new Submission();
    Submission secondSubmission = new Submission();
    firstSubmission.setFlow("testFlow");
    secondSubmission.setFlow("testFlow");
    submissionRepositoryService.save(firstSubmission);
    submissionRepositoryService.save(secondSubmission);
    assertThat(firstSubmission.getId()).isEqualTo(1);
    assertThat(secondSubmission.getId()).isEqualTo(2);
  }

  @Test
  void shouldSaveSubmission() {
    var inputData = Map.of(
        "testKey", "this is a test value",
        "otherTestKey", List.of("A", "B", "C"),
        "household", List.of(Map.of("firstName", "John", "lastName", "Perez")));
    var timeNow = Instant.now();
    var submission = Submission.builder()
        .inputData(inputData)
        .flow("testFlow")
        .submittedAt(Date.from(timeNow))
        .build();

    submissionRepositoryService.save(submission);

    Optional<Submission> savedSubmissionOptional = submissionRepositoryService.findById(
        submission.getId());
    Submission savedSubmission = savedSubmissionOptional.orElseThrow();
    assertThat(savedSubmission.getFlow()).isEqualTo("testFlow");
    assertThat(savedSubmission.getInputData()).isEqualTo(inputData);
    assertThat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(savedSubmission.getSubmittedAt()))
        .isEqualTo(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Timestamp.from(timeNow)));
  }

  @Test
  void shouldUpdateExistingSubmission() {
    var inputData = Map.of(
        "testKey", "this is a test value",
        "otherTestKey", List.of("A", "B", "C"));
    var timeNow = Instant.now();
    var submission = Submission.builder()
        .inputData(inputData)
        .flow("testFlow")
        .submittedAt(Date.from(timeNow))
        .build();
    submissionRepositoryService.save(submission);

    var newInputData = Map.of(
        "newKey", "this is a new value",
        "otherNewKey", List.of("X", "Y", "Z"));
    submission.setInputData(newInputData);
    submissionRepositoryService.save(submission);

    Optional<Submission> savedSubmissionOptional = submissionRepositoryService.findById(
        submission.getId());

    Submission savedSubmission = savedSubmissionOptional.orElseThrow();
    assertThat(savedSubmission.getInputData()).isEqualTo(newInputData);
  }

  @Test
  void shouldRemoveCSRFFromInputData() {
    var inputData = new HashMap<String, Object>();
    inputData.put("testKey", "this is a test value");
    inputData.put("otherTestKey", List.of("A", "B", "C"));
    inputData.put("_csrf", "a1fd9167-9d6d-4298-b9x4-2fc6c75ff3ab");
    var submission = Submission.builder()
        .inputData(inputData)
        .flow("testFlow")
        .build();

    submissionRepositoryService.removeFlowCSRF(submission);

    assertThat(submission.getInputData().containsKey("_csrf")).isFalse();
    assertThat(submission.getInputData().containsKey("testKey")).isTrue();
  }

  @Test
  void shouldRemoveCSRFFromSubflowInputData() {
    var inputData = new HashMap<String, Object>();
    ArrayList<Map<String, Object>> subflowArr = new ArrayList<>();
    var subflowMap = new HashMap<String, Object>();
    subflowMap.put("_csrf", "a1fd9167-9d6d-4298-b9x4-2fc6c75ff3ab");
    subflowMap.put("foo", "bar");
    subflowArr.add(subflowMap);
    inputData.put("household", subflowArr);
    var submission = Submission.builder()
        .inputData(inputData)
        .flow("testFlow")
        .build();

    submissionRepositoryService.removeSubflowCSRF(submission, "household");

    var subflowEntry = (ArrayList<Map<String, Object>>) submission.getInputData().get("household");
    assertThat(subflowEntry.get(0).containsKey("_csrf")).isFalse();
    assertThat(subflowEntry.get(0).containsKey("foo")).isTrue();
  }
}
