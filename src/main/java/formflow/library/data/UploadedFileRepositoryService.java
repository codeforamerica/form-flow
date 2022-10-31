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
   * @param uploadedFile the uploadedFile to save, not null
   */
  public void save(UploadedFile uploadedFile) {
    repository.save(uploadedFile);
  }

  /**
   * Searches for a particular Submission by its {@code id}
   *
   * @param id id of submission to look for, not null
   * @return Optional containing Submission if found, else empty
   */
  public Optional<UploadedFile> findById(Long id) {
    return repository.findById(id);
  }
}
