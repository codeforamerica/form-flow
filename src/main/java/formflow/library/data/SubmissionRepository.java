package formflow.library.data;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for the SubmissionRepository.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    boolean existsByShortCode(String shortCode);

    Optional<Submission> findSubmissionByShortCode(String shortCode);

    @Query("SELECT s.shortCode AS shortCode FROM Submission s WHERE s.id = ?1")
    String findShortCodeById(UUID uuid);
}
