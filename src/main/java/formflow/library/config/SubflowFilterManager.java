package formflow.library.config;

import formflow.library.config.submission.SubflowRelationshipFilter;
import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Looks up and runs the {@link SubflowRelationshipFilter} beans configured on subflow relationships, by the
 * filter's simple class name.
 */
@Component
public class SubflowFilterManager {

    private final HashMap<String, SubflowRelationshipFilter> filters = new HashMap<>();

    /**
     * Indexes every {@link SubflowRelationshipFilter} bean in the application context by its simple class name,
     * so it can later be looked up by name.
     *
     * @param conditionsList every {@link SubflowRelationshipFilter} bean in the application context
     */
    public SubflowFilterManager(List<SubflowRelationshipFilter> conditionsList) {
        conditionsList.forEach(filter -> this.filters.put(filter.getClass().getSimpleName(), filter));
    }

    private SubflowRelationshipFilter getFilter(String filterName) {
        return filters.get(filterName);
    }

    /**
     * Checks whether a {@link SubflowRelationshipFilter} is registered under the given name.
     *
     * @param filterName the simple class name of the filter to check for
     * @return true if a filter is registered under that name, else false
     */
    public Boolean filterExists(String filterName) {
        return filters.containsKey(filterName);
    }

    /**
     * Runs a named filter against a subflow's data.
     *
     * @param subflowDataToFilter the subflow data to filter (a copy - the original submission data is untouched)
     * @param filterName          the simple class name of the filter to run
     * @param submission          the submission the subflow data belongs to, not null
     * @return the filtered subflow data
     * @throws IllegalArgumentException if no filter is registered under {@code filterName}
     */
    public List<HashMap<String, Object>> runFilter(List<HashMap<String, Object>> subflowDataToFilter, String filterName,
            Submission submission) {
        if (!filterExists(filterName)) {
            throw new IllegalArgumentException("Subflow Relationship Filter " + filterName
                    + " does not exist. Do you have a typo in your yaml configuration?");
        }
        SubflowRelationshipFilter subflowRelationshipFilter = getFilter(filterName);
        return subflowRelationshipFilter.filter(subflowDataToFilter, submission);
    }
}
