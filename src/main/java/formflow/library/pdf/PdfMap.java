package formflow.library.pdf;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One flow's PDF field mapping configuration, as loaded from a {@code pdf-map.yaml} file: which PDF template to
 * fill, and how each of a submission's input fields, database-derived fields, and subflow fields map onto that
 * PDF's own field names.
 */
@Data
@AllArgsConstructor
public class PdfMap {

    String flow;
    String pdf;
    Map<String, Object> inputFields;
    Map<String, Object> dbFields;
    Map<String, PdfMapSubflow> subflowInfo;
    Map<String, Object> allFields;
    /**
     * Default constructor.
     */
    public PdfMap() {
    }

    /**
     * Sets the mapping from submission input field names to PDF field names, and refreshes {@link #allFields}.
     *
     * @param inputFields the input field mapping
     */
    public void setInputFields(Map<String, Object> inputFields) {
        this.inputFields = inputFields;
        updateAllFields();
    }

    /**
     * Sets the mapping from submission database-derived field names (e.g. {@code submittedAt}) to PDF field
     * names, and refreshes {@link #allFields}.
     *
     * @param dbFields the database field mapping
     */
    public void setDbFields(Map<String, Object> dbFields) {
        this.dbFields = dbFields;
        updateAllFields();
    }

    /**
     * Sets the subflow field mapping configuration, and refreshes {@link #allFields}.
     *
     * @param subflowInfo the subflow field mapping, keyed by subflow name
     */
    public void setSubflowInfo(Map<String, PdfMapSubflow> subflowInfo) {
        this.subflowInfo = subflowInfo;
        updateAllFields();
    }

    private void updateAllFields() {
        if (allFields == null) {
            allFields = new HashMap<>();
        } else {
            allFields.clear();
        }
        if (inputFields != null) {
            allFields.putAll(inputFields);
        }
        if (dbFields != null) {
            allFields.putAll(dbFields);
        }
        if (subflowInfo != null) {
            allFields.putAll(getAllSubflowFields());
        }
    }

    /**
     * Fetches and returns all the fields for all the subflows, expanding out the fields based on the number of max iterations the
     * PdfMap indicates are necessary.
     *
     * @return All the subflow fields from each subflow iteration flat-mapped to their values.  Each subflow iteration will be
     * flat-mapped to the iteration's fields suffixed with a "_" and an iteration number. This helps keep all the subflow
     * iterations together while we flatten the data.
     */
    public Map<String, Object> getAllSubflowFields() {
        Map<String, Object> subflowFields = new HashMap<>();

        subflowInfo.forEach((subflowName, subflow) -> {
            subflowFields.putAll(subflow.getFieldsForIterations());
        });

        return subflowFields;
    }
}
