package formflow.library.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve and store Submission objects in the database.
 */
//@Service
//@Transactional
public abstract class SubmissionRepositoryService {

  protected SubmissionRepository repository;

  public SubmissionRepositoryService(SubmissionRepository repository) {
    this.repository = repository;
  }


}
