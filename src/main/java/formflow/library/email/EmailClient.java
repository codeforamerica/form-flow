package formflow.library.email;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.List;

public interface EmailClient {

    void sendEmail();

    void sendEmail(String subject,
                   String senderEmail,
                   String recipientEmail,
                   String emailBody);

    void sendEmail(String subject,
                   String senderEmail,
                   String recipientEmail,
                   String emailBody,
                   List<PDDocument> attachments);

    void sendEmail(
            String subject,
            String senderEmail,
            String recipientEmail,
            List<String> emailsToCC,
            String emailBody,
            List<PDDocument> attachments,
            boolean requireTls);
}
