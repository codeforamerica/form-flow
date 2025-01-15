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

    boolean existsByShortCode(String shortCode);

    Optional<Submission> findSubmissionByShortCode(String shortCode);

    Optional<Submission> findProviderSubmissionFromFamilySubmission(Submission familySubmission);
}
