package formflow.library.inputs;

/**
 * Prefixes/suffixes used to recognize special-purpose form field names within submitted form data - fields that
 * shouldn't be run through normal bean validation, or that carry address-validation/dynamic-input metadata.
 */
public class FieldNameMarkers {

    /**
     * Marks the CSRF token field, which is excluded from bean validation.
     */
    public static final String UNVALIDATED_FIELD_MARKER_CSRF = "_csrf";
    /**
     * Prefix on a submitted field name indicating it's part of an address-validation suggestion response (e.g.
     * {@code validate_streetAddress1}), and so is excluded from normal bean validation.
     */
    public static final String UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS = "validate_";
    /**
     * Suffix appended to an address's sub-field names (street address, city, state, zip) once address validation
     * has resolved a suggested address, marking them as already validated.
     */
    public static final String UNVALIDATED_FIELD_MARKER_VALIDATED = "_validated";
    /**
     * Marker within a dynamically-named input field's name (e.g. for a repeated/array input), used to extract the
     * base field name for validation configuration lookups.
     */
    public static final String DYNAMIC_FIELD_MARKER = "_wildcard_";
    /**
     * Default constructor.
     */
    public FieldNameMarkers() {
    }
}
