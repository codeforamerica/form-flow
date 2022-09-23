package formflow.library.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class SubmissionRepositoryService {

  @Autowired
  SubmissionRepository repository;

  public void save(Submission submission) {
    repository.save(submission);
  }

  public Optional<Submission> findById(Long id) {
    return repository.findById(id);
  }

  public void removeFlowCSRF(Submission submission) {
    submission.getInputData().remove("_csrf");
  }

  public void removeSubflowCSRF(Submission submission, String subflowName) {
    var subflowArr = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);

    if (subflowArr != null) {
      for (var entry : subflowArr) {
        entry.remove("_csrf");
      }
    }
  }
}
