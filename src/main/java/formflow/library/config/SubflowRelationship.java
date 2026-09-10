package formflow.library.config;

import lombok.Data;

/**
 * Configures how one subflow relates to another: {@code relatesTo} names the related subflow, {@code filter}
 * optionally names a {@link formflow.library.config.submission.SubflowRelationshipFilter} used to select which
 * of the related subflow's iterations apply, and {@code repeatFor} optionally configures a nested, repeated
 * sub-iteration within each matched entry.
 */
@Data
public class SubflowRelationship {

    private String relatesTo;
    private String relationAlias;
    private String filter;
    private RepeatFor repeatFor;
    /**
     * Default constructor.
     */
    public SubflowRelationship() {
    }
}
