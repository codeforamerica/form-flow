package formflow.library.config;

import lombok.Data;

@Data
public class SubflowRelationship {
    /**
     * Default constructor.
     */
    public SubflowRelationship() {
    }
    
    private String relatesTo;
    private String relationAlias;
    private String filter;
    private RepeatFor repeatFor;
}
