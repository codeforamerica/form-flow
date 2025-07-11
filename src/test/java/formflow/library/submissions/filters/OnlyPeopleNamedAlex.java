package formflow.library.submissions.filters;

import formflow.library.config.submission.SubflowRelationshipFilter;
import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OnlyPeopleNamedAlex implements SubflowRelationshipFilter {
    
    @Override
    public List<HashMap<String, Object>> filter(List<HashMap<String, Object>> subflowDataToFilter, Submission submission) {
        return subflowDataToFilter.stream().filter(data -> "Alex".equals(data.get("householdMemberFirstName"))).toList();
    }
}
