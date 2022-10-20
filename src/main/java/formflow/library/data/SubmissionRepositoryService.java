package formflow.library.data;

import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Service to retrieve and store Submission objects in the database.
 */
@Service
@Transactional
public class SubmissionRepositoryService {

    SubmissionRepository repository;

    public SubmissionRepositoryService(SubmissionRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves the Submission in the database.
     *
     * @param submission the submission to save, not null
     */
    public void save(Submission submission) {
        repository.save(submission);
    }

    /**
     * Searches for a particular Submission by its {@code id}
     *
     * @param id id of submission to look for, not null
     * @return Optional containing Submission if found, else empty
     */
    public Optional<Submission> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Removes the CSRF from the Submission's input data, if found.
     *
     * @param submission submission to remove the CSRF from, not null
     */
    public void removeFlowCSRF(Submission submission) {
        submission.getInputData().remove("_csrf");
    }

    /**
     * Removes the CSRF from a particular Submission's Subflow's iteration data, if found.
     * <p>
     * This will remove the CSRF from all the iterations in the subflow.
     * </p>
     *
     * @param submission  submission to look for subflows in, not null
     * @param subflowName the subflow to remove the CSRF from, not null
     */
    public void removeSubflowCSRF(Submission submission, String subflowName) {
        var subflowArr = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);

        if (subflowArr != null) {
            for (var entry : subflowArr) {
                entry.remove("_csrf");
            }
        }
    }
}
