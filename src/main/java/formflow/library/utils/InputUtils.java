package formflow.library.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;

/**
 * Utility class to help with parsing input from forms
 */
@Slf4j
public class InputUtils {

    /**
     * Static function to help determine if a String or ArrayList contains the {@code target} value.
     *
     * <p>
     * Currently this performs a comparison between two data types:
     *     <ul>
     *         <li>String - checks if the {@code value} equals the {@code target} </li>
     *         <li>ArrayList of Strings - checks if the {@code target} is in the ArrayList</li>
     *     </ul>
     * </p>
     *
     * @param value  Object to check if it equals or contains the {@code target} string
     * @param target string to find in {@code target}
     * @return true if the {@code target} is equal to or contained in {@code value}, else false
     */
    public static boolean arrayOrStringContains(Object value, String target) {
        if (value instanceof String) {
            return value.equals(target);
        }

        if (value instanceof ArrayList) {
            return ((ArrayList<?>) value).contains(target);
        }

        return false;
    }

    /**
     * Static function that returns a Map of input data for use in the thymeleaf template. If you are in a subflow,
     * the Map will be set to currentSubflowItem and if you are not in a subflow, the Map will be set to
     * inputData (submission inputData).
     *
     * @param currentSubflowItem a Map of input field name to value specific to the current subflow iteration, can be null.
     * @param inputData          a Map of all input field names to values held in the submission object, can but probably shouldn't be null.
     * @return currentSubflowItem if it is not null, otherwise inputData.
     */
    public static Map<String, Object> getFieldData(Map<String, Object> currentSubflowItem, Map<String, Object> inputData) {
        if (currentSubflowItem != null) {
            return currentSubflowItem;
        } else if (inputData != null) {
            return inputData;
        }
        log.warn("No data could be found for use in templates.");
        return Map.of();
    }
}