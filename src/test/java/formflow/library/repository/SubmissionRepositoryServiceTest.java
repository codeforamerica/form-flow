package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
class SubmissionRepositoryServiceTest {

  @Autowired
  private SubmissionRepositoryService submissionRepositoryService;

  @PersistenceContext
  EntityManager entityManager;

  @Test
  void shouldSaveASubmissionWithUUID() {
    Submission firstSubmission = new Submission();
    firstSubmission.setFlow("testFlow");

    submissionRepositoryService.save(firstSubmission);

    assertThat(firstSubmission.getId()).isInstanceOf(UUID.class);
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
        .urlParams(new HashMap<>())
        .flow("testFlow")
        .submittedAt(Date.from(timeNow))
        .build();

    UUID submissionId = submissionRepositoryService.save(submission);

    Optional<Submission> savedSubmissionOptional = submissionRepositoryService.findById(submissionId);
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
        .urlParams(new HashMap<>())
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

  @Test
  void findByIdShouldReturnsDecryptedField() {
    var inputData = Map.of(
        "testKey", "this is a test value",
        "otherTestKey", List.of("A", "B", "C"),
        "ssnInput", "123-45-6789",
        "household", List.of(Map.of("firstName", "John", "lastName", "Perez", "ssnInputSubflow", "321-54-9876")));
    var timeNow = Instant.now();
    var submission = Submission.builder()
        .inputData(inputData)
        .urlParams(new HashMap<>())
        .flow("testFlow")
        .submittedAt(Date.from(timeNow))
        .build();

    UUID subId = submissionRepositoryService.save(submission);

    Submission dbSubmission = (submissionRepositoryService.findById(subId)).get();
    assertThat(dbSubmission.getInputData().containsKey("ssnInput")).isTrue();
    assertThat(dbSubmission.getInputData().containsKey("ssnInput_encrypted")).isFalse();
    assertThat(dbSubmission.getInputData().get("ssnInput")).isEqualTo("123-45-6789");

    Map<String, Object> subflowData = (Map) ((List) dbSubmission.getInputData().get("household")).get(0);
    assertThat(subflowData.containsKey("ssnInputSubflow")).isTrue();
    assertThat(subflowData.containsKey("ssnInputSubflow_encrypted")).isFalse();
    assertThat(subflowData.get("ssnInputSubflow")).isEqualTo("321-54-9876");
  }

  @Test
  void saveShouldEncryptFieldInDB() {
    var inputData = Map.of(
        "testKey", "this is a test value",
        "otherTestKey", List.of("A", "B", "C"),
        "ssnInput", "123-45-6789",
        "household", List.of(Map.of("firstName", "John", "lastName", "Perez", "ssnInputSubflow", "321-54-9876")));
    var timeNow = Instant.now();
    var submission = Submission.builder()
        .inputData(inputData)
        .urlParams(new HashMap<>())
        .flow("testFlow")
        .submittedAt(Date.from(timeNow))
        .build();

    UUID subId = submissionRepositoryService.save(submission);

    var query = entityManager.createQuery("SELECT s FROM Submission s WHERE s.id = :id");
    query.setParameter("id", subId);
    Submission resultSubmission = (Submission) query.getSingleResult();

    // sanity check to ensure we got the correct submission
    assertThat(resultSubmission.getId()).isEqualTo(subId);

    // check first level ssn
    assertThat(resultSubmission.getInputData().containsKey("ssnInput")).isFalse();
    assertThat(resultSubmission.getInputData().containsKey("ssnInput_encrypted")).isTrue();
    assertThat(resultSubmission.getInputData().get("ssnInput_encrypted")).isNotEqualTo(submission.getInputData().get("ssnInput"));

    // check subflow ssn field
    Map<String, Object> resultHouseholdSubflow =
        (Map<String, Object>) ((List) resultSubmission.getInputData().get("household")).get(0);
    Map<String, Object> origHouseholdSubflow = (Map<String, Object>) ((List) submission.getInputData().get("household")).get(0);
    assertThat(resultHouseholdSubflow.containsKey("ssnInputSubflow_encrypted")).isTrue();
    assertThat(resultHouseholdSubflow.containsKey("ssnInputSubflow")).isFalse();
    assertThat(resultHouseholdSubflow.get("ssnInputSubflow_encrypted")).isNotEqualTo(origHouseholdSubflow.get("ssnInputSubflow"));
  }

  @Test
  void shouldSetCreatedAtAndUpdatedAtFields() {
    var inputData = Map.of(
        "testKey", "this is a test value",
        "otherTestKey", List.of("A", "B", "C")
    );
    var timeNow = Instant.now();
    var submission = Submission.builder()
        .inputData(inputData)
        .urlParams(new HashMap<>())
        .flow("testFlow")
        .build();

    UUID id = submissionRepositoryService.save(submission);
    Submission savedSubmission = submissionRepositoryService.findById(id).get();

    assertThat(savedSubmission.getCreatedAt()).isInThePast();
    assertThat(savedSubmission.getUpdatedAt()).is(updatedAt -> updatedAt != null);
    assertThat(savedSubmission.getUpdatedAt() == null).isTrue();

    inputData.put("newKey", "newValue");

  }
}
