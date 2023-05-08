package formflow.library;

import formflow.library.email.MailgunEmailClient;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

// TODO: delete
@Controller
@EnableAutoConfiguration
@Slf4j
@RequestMapping("/email")
public class EmailController {

    private final MailgunEmailClient mailgunEmailClient;

    public EmailController(MailgunEmailClient mailgunEmailClient) {
        this.mailgunEmailClient = mailgunEmailClient;
    }

    @GetMapping("/test")
    ResponseEntity<String> sendTestEmail(
            HttpSession httpSession
    ) throws IOException {
        log.info("/email sendTestEmail ✨");
        HttpHeaders headers = new HttpHeaders();
        mailgunEmailClient.sendEmail();
        return new ResponseEntity<>("Send test email", headers, HttpStatus.OK);
    }
}
