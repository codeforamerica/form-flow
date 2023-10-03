package formflow.library.data;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve and store UploadedFile objects in the database.
 */
@Service
@Transactional
@Slf4j
public class UserFileRepositoryService {

  UserFileRepository repository;

  public UserFileRepositoryService(UserFileRepository repository) {
    this.repository = repository;
  }

  /**
   * Saves the UploadedFile in the database.
   *
   * @param userFile the uploadedFile to save, not null
   * @return UUID of the file
   */
  public UserFile save(UserFile userFile) {
    return repository.save(userFile);
  }

  /**
   * Searches for a particular UserFile by its {@code id}
   *
   * @param id UUID of the UserFile to look for, not null
   * @return Optional containing UserFile if found, else empty
   */
  public Optional<UserFile> findById(UUID id) {
    return repository.findById(id);
  }

  /**
   * Removes a particular UserFile based on passed in {@code id}
   *
   * @param id UUID of UserFile to remove, not null
   */
  public void deleteById(UUID id) {
    log.info(String.format("Deleting file with id: '%s'", id));
    repository.deleteById(id);
  }

  public List<UserFile> findAllBySubmission(Submission submission) {
    return repository.findAllBySubmission(submission);
  }

  public long countBySubmission(Submission submission) {
    return repository.countBySubmission(submission);
  }
}
