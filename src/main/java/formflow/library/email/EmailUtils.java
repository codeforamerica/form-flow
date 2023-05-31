package formflow.library.email;

public class EmailUtils {

  public static String wrapHtml(String message) {
    return "<html><body>%s</body></html>".formatted(message);
  }
}
