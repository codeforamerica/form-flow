package formflow.library.data;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for the SubmissionRepository.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    boolean existsByShortCode(String shortCode);

    @Query("SELECT s FROM Submission s WHERE UPPER(s.shortCode) = UPPER(:shortCode)")
    Optional<Submission> findSubmissionByShortCode(@Param("shortCode") String shortCode);
}
