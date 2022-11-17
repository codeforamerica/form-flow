package formflow.library.data;

import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve and store UploadedFile objects in the database.
 */
@Service
@Transactional
public class UploadedFileRepositoryService {

  UploadedFileRepository repository;

  public UploadedFileRepositoryService(UploadedFileRepository repository) {
    this.repository = repository;
  }

  /**
   * Saves the UploadedFile in the database.
   *
   * @param userFile the uploadedFile to save, not null
   */
  public Long save(UserFile userFile) {
    return repository.save(userFile).getFile_id();
  }

  /**
   * Searches for a particular Submission by its {@code id}
   *
   * @param id id of submission to look for, not null
   * @return Optional containing Submission if found, else empty
   */
  public Optional<UserFile> findById(Long id) {
    return repository.findById(id);
  }
}
