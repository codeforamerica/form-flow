package formflow.library.email;

import java.io.File;
import java.util.List;

public interface EmailClient {

    /**
     * The smallest amount of information to give to send an email.
     *
     * @param subject        The subject line of the email
     * @param recipientEmail The email address that will get the email, aka the To field
     * @param emailBody      The plain text version of the email body
     */
    void sendEmail(
            String subject,
            String recipientEmail,
            String emailBody
    );

    /**
     * The smallest amount of information plus attachments.
     *
     * @param subject        The subject line of the email
     * @param recipientEmail The email address that will get the email, aka the To field
     * @param emailBody      The plain text version of the email body
     * @param attachments    A list of files that will be added as attachments to the email
     */
    void sendEmail(
            String subject,
            String recipientEmail,
            String emailBody,
            List<File> attachments
    );

    /**
     * The most customizable method to send email.
     *
     * @param subject        The subject line of the email
     * @param recipientEmail The email address that will get the email, aka the To field
     * @param emailsToCC     A list of emails to be added into the CC field
     * @param emailsToBCC    A list of emails to be added into the BCC field
     * @param emailBody      The plain text version of the email body
     * @param attachments    A list of files that will be added as attachments to the email
     * @param requireTls     A way to make TLS required
     */
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
