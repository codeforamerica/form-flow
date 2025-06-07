package formflow.library.config;

import lombok.Data;

@Data
public class SubflowRelationship {
    
    private String relatesTo;
    private String relationAlias;
    private String filter;
}
