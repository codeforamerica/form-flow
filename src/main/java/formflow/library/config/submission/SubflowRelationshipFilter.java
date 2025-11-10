package formflow.library.config.submission;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;

/**
 * An interface that defines a filtering function to filter data in a subflow relationship context.
 *
 * <p>
 * The filter is applied to a subflow relationship configuration allowing for filtering iterations of a related subflow when
 * defining a relationship between two subflows.
 * </p>
 */
public interface SubflowRelationshipFilter {

    /**
     * Runs a filter method against a specific subflows data.
     *
     * @param subflowDataToFilter The subflow data to be filtered. Note that this is a copy and will not modify the original data
     *                            in the submission. Filtering will only occur against the copy when setting up the data for the
     *                            subflow relationship.
     * @return The filtered subflow data.
     */
    default List<HashMap<String, Object>> filter(List<HashMap<String, Object>> subflowDataToFilter, Submission submission) {
        throw new UnsupportedOperationException(
                "You did not implement the necessary filter method for your implementing class of the SubflowRelationshipFilter interface: "
                        + this.getClass().getName());
    }
}
