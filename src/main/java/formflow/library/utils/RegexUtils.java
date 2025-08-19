package formflow.library.utils;

/**
 * Utility class containing regular expressions for various patterns. This class serves as a centralized repository for regex
 * patterns used throughout the application.
 */
public class RegexUtils {

    /**
     * Regular expression pattern for validating email addresses. This pattern conforms to the general email format, allowing a
     * wide range of email addresses. It includes support for various characters in the local part and domain of the email
     * address.
     *
     * @see <a
     * href="https://github.com/codeforamerica/shiba/blob/f3f241e01cf72587813f312c086b557c2412210e/src/main/java/org/codeforamerica/shiba/pages/config/Validation.java#L59-L60">Email
     * regex is from Shiba.</a>
     */
    public static final String EMAIL_REGEX = "[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";
}
