package formflow.library.data;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to retrieve and store Submission objects in the database.
 */
@Service
@Transactional
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
   * @param submission the submission to save, not null
   * @return UUID of the saved submission
   */
  public UUID save(Submission submission) {
    UUID id = repository.save(encryptionService.encrypt(submission)).getId();
    submission.setId(id);
    return id;
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

  /**
   * If the submission exists in the session, find it in the db. If not or can't be found, create a new one.
   *
   * @param httpSession submission
   * @return Submission
   */
  public Submission findOrCreate(HttpSession httpSession) {
    var id = (UUID) httpSession.getAttribute("id");
    if (id != null) {
      Optional<Submission> submissionOptional = findById(id);
      if (submissionOptional.isEmpty()) {
        log.error("findOrCreate could not find submission: " + id);
        Submission newSubmission = new Submission();
        log.info("findOrCreate created new submission: " + newSubmission.getId());
        return newSubmission;
      } else {
        return submissionOptional.get();
      }
    } else {
      Submission newSubmission = new Submission();
      log.info("findOrCreate got no submission id from session, so created new submission: " + newSubmission.getId());
      return newSubmission;
    }
  }
}
