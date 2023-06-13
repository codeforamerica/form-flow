package formflow.library.email;

import java.io.File;
import java.util.List;

public interface EmailClient<T> {

  /**
   * This sends an email with the least amount of information needed to be provided.
   *
   * @param subject        The subject line of the email
   * @param recipientEmail The email address that will get the email, aka the To field
   * @param emailBody      The plain text version of the email body
   * @return A generic T
   */
  T sendEmail(
      String subject,
      String recipientEmail,
      String emailBody
  );

  /**
   * This sends an email with the least amount of information needed to be provided, but with attachments.
   *
   * @param subject        The subject line of the email
   * @param recipientEmail The email address that will get the email, aka the To field
   * @param emailBody      The plain text version of the email body
   * @param attachments    A list of files that will be added as attachments to the email
   * @return A generic T
   */
  T sendEmail(
      String subject,
      String recipientEmail,
      String emailBody,
      List<File> attachments
  );

  /**
   * This sends an email with the most customizations.
   *
   * @param subject        The subject line of the email
   * @param recipientEmail The email address that will get the email, aka the To field
   * @param emailsToCC     A list of emails to be added into the CC field
   * @param emailsToBCC    A list of emails to be added into the BCC field
   * @param emailBody      The plain text version of the email body
   * @param attachments    A list of files that will be added as attachments to the email
   * @param requireTls     A way to make TLS required
   * @return A generic T
   */
  T sendEmail(
      String subject,
      String recipientEmail,
      List<String> emailsToCC,
      List<String> emailsToBCC,
      String emailBody,
      List<File> attachments,
      boolean requireTls
  );
}
