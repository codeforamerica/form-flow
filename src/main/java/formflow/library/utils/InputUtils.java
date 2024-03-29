package formflow.library.utils;

import java.util.ArrayList;

/**
 * Utility class to help with parsing input from forms
 */
public class InputUtils {

  /**
   * Static function to help determine if a String or ArrayList contains the {@code target} value.
   *
   * <p>Currently, this performs a comparison between two data types:</p>
   * <ul>
   *     <li>String - checks if the {@code value} equals the {@code target} </li>
   *     <li>ArrayList of Strings - checks if the {@code target} is in the ArrayList</li>
   * </ul>
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
}
