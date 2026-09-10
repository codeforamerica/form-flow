package formflow.library.data;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for the SubmissionRepository.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * Checks whether a submission already exists with the given short code.
     *
     * @param shortCode the short code to check
     * @return true if a submission already exists with this short code
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Searches for a submission by its short code.
     *
     * @param shortCode the short code to search for
     * @return the submission with this short code, if one exists
     */
    Optional<Submission> findSubmissionByShortCode(String shortCode);
}
