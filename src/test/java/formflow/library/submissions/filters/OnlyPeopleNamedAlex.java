package formflow.library.submissions.filters;

import formflow.library.config.submission.SubflowRelationshipFilter;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OnlyPeopleNamedAlex implements SubflowRelationshipFilter {
    
    @Override
    public List<HashMap<String, Object>> filter(List<HashMap<String, Object>> subflowDataToFilter) {
        return subflowDataToFilter.stream().filter(data -> "Alex".equals(data.get("householdMemberFirstName"))).toList();
    }
}
