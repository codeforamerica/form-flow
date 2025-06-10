package formflow.library.config;

import formflow.library.config.submission.SubflowRelationshipFilter;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SubflowFilterManager {
    private final HashMap<String, SubflowRelationshipFilter> filters = new HashMap<>();

    public SubflowFilterManager(List<SubflowRelationshipFilter> conditionsList) {
        conditionsList.forEach(filter -> this.filters.put(filter.getClass().getSimpleName(), filter));
    }
    
    private SubflowRelationshipFilter getFilter(String filterName) {
        return filters.get(filterName);
    }
    
    public Boolean filterExists(String filterName) {
        return filters.containsKey(filterName);
    }
    
    public List<HashMap<String, Object>> runFilter(List<HashMap<String, Object>> subflowDataToFilter, String filterName) {
        if (!filterExists(filterName)) {
            throw new IllegalArgumentException("Subflow Relationship Filter " + filterName + " does not exist. Do you have a typo in your yaml configuration?");
        }
        SubflowRelationshipFilter subflowRelationshipFilter = getFilter(filterName);
        return subflowRelationshipFilter.filter(subflowDataToFilter);
    }
}
