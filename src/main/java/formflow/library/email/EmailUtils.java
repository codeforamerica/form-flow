package formflow.library.email;

public class EmailUtils {

  /**
   * Converts message to html, allowing for styling of message.
   *
   * @param message - the text and markup for an email's body.
   * @return - html for an email's body
   */
  public static String wrapHtml(String message) {
    return "<html><body>%s</body></html>".formatted(message);
  }
}
