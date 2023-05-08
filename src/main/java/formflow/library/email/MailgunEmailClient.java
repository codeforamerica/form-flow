package formflow.library.email;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import lombok.Value;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MailgunEmailClient implements EmailClient {

    private final String senderEmail;
    private final String mailgunKey;
    private final String mailgunDomain;

    public MailgunEmailClient(@Value("${form-flow.email-client.mailgun.sender-email:}") String senderEmail,
                              @Value("${form-flow.email-client.mailgun.domain:}") String mailgunDomain,
                              @Value("${form-flow.email-client.mailgun.key:}") String mailgunKey
    ) {
        this.senderEmail = senderEmail;
        this.mailgunKey = mailgunKey;
        this.mailgunDomain = mailgunDomain;
    }

    @Override
    public void sendEmail() {
        // Mailgun API - https://github.com/mailgun/mailgun-java
//        MailgunClient.config(mailgunKey);
        MailgunMessagesApi mailgunMessagesApi = MailgunClient.config(mailgunKey)
                .createApi(MailgunMessagesApi.class);
        Message message = Message.builder()
                .from(senderEmail)
                .to("cborg@codeforamerica.org")
                .subject("Subject \uD83D\uDD25")
                .text("This is a test \uD83D\uDC38")
                .build();

        MessageResponse messageResponse = mailgunMessagesApi.sendMessage(mailgunDomain, message);
    }

    @Override
    public void sendEmail(String subject,
                          String senderEmail,
                          String recipientEmail,
                          String emailBody) {
//        sendEmail(subject,
//                senderEmail,
//                recipientEmail,
//                emptyList(),
//                emailBody,
//                emptyList(),
//                false);
    }

    @Override
    public void sendEmail(String subject,
                          String senderEmail,
                          String recipientEmail,
                          String emailBody,
                          List<PDDocument> attachments) {
//        sendEmail(subject,
//                senderEmail,
//                recipientEmail,
//                emptyList(),
//                emailBody,
//                attachments,
//                false);
    }

    @Override
    public void sendEmail(
            String subject,
            String senderEmail,
            String recipientEmail,
            List<String> emailsToCC,
            String emailBody,
            List<PDDocument> attachments,
            boolean requireTls) {

//        🐕 Shiba way 🦮
//        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
//        form.put("from", List.of(senderEmail));
//        form.put("to", List.of(recipientEmail));
//        form.put("subject", List.of(subject));
//        form.put("html", List.of(emailBody));
//        if (!attachments.isEmpty()) {
//            form.put("attachment", attachments.stream().map(this::asResource).collect(toList()));
//        }
//        if (!emailsToCC.isEmpty()) {
//            form.put("cc", new ArrayList<>(emailsToCC)); // have to create new list of type List<Object>
//        }
//        if (requireTls) {
//            form.put("o:require-tls", List.of("true"));
//        }
//
//        webClient.post()
//                .headers(httpHeaders -> httpHeaders.setBasicAuth("api", mailGunApiKey))
//                .body(fromMultipartData(form))
//                .retrieve()
//                .bodyToMono(Void.class)
//                .block();
    }

//    @NotNull
//    private Resource asResource(ApplicationFile applicationFile) {
//        return new InMemoryResource(applicationFile.getFileBytes()) {
//            @Override
//            public String getFilename() {
//                return applicationFile.getFileName();
//            }
//        };
//    }
}
