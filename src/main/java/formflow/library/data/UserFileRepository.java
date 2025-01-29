package formflow.library.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for UserFile objects
 */
@Repository
public interface UserFileRepository extends JpaRepository<UserFile, UUID> {

  /**
   * Finds all the {@link UserFile}s associated with a {@link Submission}}
   *
   * @param submission the {@link Submission} for which the associated {@link UserFile}s are sought
   * @return {@link List} of associated {@link UserFile} objects
   */
  List<UserFile> findAllBySubmission(Submission submission);

  /**
   * Gets a count of all the {@link UserFile}s associated with a {@link Submission}} where the
   * file has never been converted
   *
   * @param submission the {@link Submission} for which the count of associated {@link UserFile}s are sought
   * @return count of {@link UserFile}s
   */
  long countBySubmissionAndConversionSourceFileIdIsNull(Submission submission);
}
