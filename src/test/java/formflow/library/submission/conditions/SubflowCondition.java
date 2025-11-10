package formflow.library.submission.conditions;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import org.springframework.stereotype.Component;

@Component
public class SubflowCondition implements Condition {

    @Override
    public Boolean run(Submission submission, String subflowUuid) {
        return true;
    }
}
