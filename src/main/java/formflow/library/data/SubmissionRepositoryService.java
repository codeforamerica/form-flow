package formflow.library.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve and store {@link formflow.library.data.Submission} objects in the database.
 */
@Service
@Slf4j
public class SubmissionRepositoryService {

  SubmissionRepository repository;

  SubmissionEncryptionService encryptionService;

  public SubmissionRepositoryService(SubmissionRepository repository, SubmissionEncryptionService encryptionService) {
    this.repository = repository;
    this.encryptionService = encryptionService;
  }

  /**
   * Saves the Submission in the database.
   *
   * @param  submission the {@link formflow.library.data.Submission} to save, not null
   * @return the saved {@link formflow.library.data.Submission}
   */
  public Submission save(Submission submission) {
    var newRecord = submission.getId() == null;
    Submission savedSubmission = repository.save(encryptionService.encrypt(submission));
    if (newRecord) {
      log.info("created submission id: " + savedSubmission.getId());
    }
    // straight from the db will be encrypted, so decrypt first.
    return encryptionService.decrypt(savedSubmission);
  }

  /**
   * Searches for a particular Submission by its {@code id}
   *
   * @param id id of submission to look for, not null
   * @return Optional containing Submission if found, else empty
   */
  public Optional<Submission> findById(UUID id) {
    Optional<Submission> submission = repository.findById(id);
    return submission.map(value -> encryptionService.decrypt(value));
  }

  /**
   * Removes the CSRF from the Submission's input data, if found.
   *
   * @param submission submission to remove the CSRF from, not null
   */
  public void removeFlowCSRF(Submission submission) {
    submission.getInputData().remove("_csrf");
  }

  /**
   * Removes the CSRF from a particular Submission's Subflow's iteration data, if found.
   * <p>
   * This will remove the CSRF from all the iterations in the subflow.
   * </p>
   *
   * @param submission  submission to look for subflows in, not null
   * @param subflowName the subflow to remove the CSRF from, not null
   */
  public void removeSubflowCSRF(Submission submission, String subflowName) {
    var subflowArr = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);

    if (subflowArr != null) {
      for (var entry : subflowArr) {
        entry.remove("_csrf");
      }
    }
  }
}
