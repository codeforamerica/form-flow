package formflow.library.submissions.actions;

import formflow.library.config.submission.Action;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class LookupRandomSubmissionAfterSave implements Action {

    @Autowired
    private SubmissionRepository submissionRepository;

    public void run(Submission submission) {
        submissionRepository.findById(UUID.randomUUID());
    }

    public void run(Submission submission, String id) {
        submissionRepository.findById(UUID.randomUUID());
    }
}
