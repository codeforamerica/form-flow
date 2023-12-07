package formflow.library.utils;

/**
 * Utility class containing regular expressions for various patterns. This class serves as a centralized repository for regex
 * patterns used throughout the application.
 */
public class RegexUtils {

  /**
   * Regular expression pattern for validating email addresses. This pattern conforms to the general email format, allowing a wide
   * range of email addresses. It includes support for various characters in the local part and domain of the email address.
   */
  public static final String EMAIL_REGEX = "[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";
}
