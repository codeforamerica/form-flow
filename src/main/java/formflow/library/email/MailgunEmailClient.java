package formflow.library.email;

import static java.util.Collections.emptyList;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import feign.FeignException;
import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MailgunEmailClient implements EmailClient {

  private String senderEmail;
  private final String mailgunDomain;
  private MailgunMessagesApi mailgunMessagesApi;

  public MailgunEmailClient(@Value("${form-flow.email-client.mailgun.sender-email:}") String senderEmail,
      @Value("${form-flow.email-client.mailgun.domain:}") String mailgunDomain,
      @Value("${form-flow.email-client.mailgun.key:}") String mailgunKey
  ) {
    this.senderEmail = senderEmail;
    this.mailgunDomain = mailgunDomain;
    this.mailgunMessagesApi = MailgunClient.config(mailgunKey)
        .createApi(MailgunMessagesApi.class);
  }

  /**
   * This sends an email with the least amount of information needed to be provided. Sets empty or defaults to the rest of the
   * parameters.
   *
   * @param subject        The subject line of the email
   * @param recipientEmail The email address that will get the email, aka the To field
   * @param emailBody      The HTML version of the email body
   */
  @Override
  public void sendEmail(
      String subject,
      String recipientEmail,
      String emailBody) {
    sendEmail(
        subject,
        recipientEmail,
        emptyList(),
        emptyList(),
        emailBody,
        emptyList(),
        false);
  }

  /**
   * This sends an email with the least amount of information needed to be provided, but with attachments. Sets empty or defaults
   * to the rest of the parameters.
   *
   * @param subject        The subject line of the email
   * @param recipientEmail The email address that will get the email, aka the To field
   * @param emailBody      The HTML version of the email body
   * @param attachments    A list of files that will be added as attachments to the email
   */
  @Override
  public void sendEmail(
      String subject,
      String recipientEmail,
      String emailBody,
      List<File> attachments) {
    sendEmail(
        subject,
        recipientEmail,
        emptyList(),
        emptyList(),
        emailBody,
        attachments,
        false);
  }

  /**
   * The main method that sends to Mailgun. Allows all parameter customization.
   *
   * @param subject        The subject line of the email
   * @param recipientEmail The email address that will get the email, aka the To field
   * @param emailsToCC     A list of emails to be added into the CC field
   * @param emailsToBCC    A list of emails to be added into the BCC field
   * @param emailBody      The HTML version of the email body
   * @param attachments    A list of files that will be added as attachments to the email
   * @param requireTls     A way to make TLS required
   */
  @Override
  public void sendEmail(
      String subject,
      String recipientEmail,
      List<String> emailsToCC,
      List<String> emailsToBCC,
      String emailBody,
      List<File> attachments,
      boolean requireTls) {
    Message.MessageBuilder message = Message.builder()
        .from(senderEmail)
        .to(recipientEmail)
        .subject(subject)
        .html(emailBody)
        .requireTls(requireTls);

    if (emailsToCC != null && !emailsToCC.isEmpty()) {
      message.cc(emailsToCC);
    }
    if (emailsToBCC != null && !emailsToBCC.isEmpty()) {
      message.bcc(emailsToBCC);
    }
    if (attachments != null && !attachments.isEmpty()) {
      message.attachment(attachments);
    }
    Message builtMessage = message.build();

    try {
      mailgunMessagesApi.sendMessage(mailgunDomain, builtMessage);
    } catch (FeignException exception) {
      log.error(exception.getMessage());
    }
  }

  /**
   * This setter allows us to replace mailgunMessageApi with a mock for testing.
   *
   * @param mailgunMessageApi The mailgunMessageApi you want to use to interface with Mailgun.
   */
  public void setMailgunMessagesApi(MailgunMessagesApi mailgunMessageApi) {
    this.mailgunMessagesApi = mailgunMessageApi;
  }

  /**
   * This setter allows you to change the senderEmail. By default, senderEmail is defined in application.yaml.
   *
   * @param senderEmail The email that is used to fill the from field.
   */
  public void setSenderEmail(String senderEmail) {
    this.senderEmail = senderEmail;
  }
}
