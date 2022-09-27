package formflow.library.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for the SubmissionRepository.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

}
