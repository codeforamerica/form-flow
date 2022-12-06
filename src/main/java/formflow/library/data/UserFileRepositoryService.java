package formflow.library.data;

import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve and store UploadedFile objects in the database.
 */
@Service
@Transactional
public class UserFileRepositoryService {

  UserFileRepository repository;

  public UserFileRepositoryService(UserFileRepository repository) {
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
   * Searches for a particular UserFile by its {@code id}
   *
   * @param id id of the UserFile to look for, not null
   * @return Optional containing UserFile if found, else empty
   */
  public Optional<UserFile> findById(Long id) {
    return repository.findById(id);
  }

  /**
   * Removes a particular UserFile based on passed in {@code id}
   *
   * @param id id of UserFile to remove, not null
   */
  public void deleteById(Long id) {
    repository.deleteById(id);
  }
}
