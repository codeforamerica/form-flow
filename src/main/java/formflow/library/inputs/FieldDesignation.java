package formflow.library.inputs;

/**
 * A class to capture any string constants that might apply to fields in the inputs class.
 */
public class FieldDesignation {

  /**
   * A string used to indicate if an input field is a dynamic field. If this string is found in a field's name in a form
   * submission, then it's likely the field is a dynamic field.
   */
  public static final String DYNAMIC_FIELD_MARKER = "_wildcard_";

}
