package formflow.library.email;

import java.io.File;
import java.util.List;

public interface EmailClient {

  void sendEmail(
    String subject,
    String recipientEmail,
    String emailBody
  );

  void sendEmail(
    String subject,
    String recipientEmail,
    String emailBody,
    List<File> attachments
  );

  void sendEmail(
    String subject,
    String recipientEmail,
    List<String> emailsToCC,
    List<String> emailsToBCC,
    String emailBody,
    List<File> attachments,
    boolean requireTls
  );
}
