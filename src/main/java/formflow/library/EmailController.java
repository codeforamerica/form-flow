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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

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
    HttpHeaders headers = new HttpHeaders();
    mailgunEmailClient.sendEmail(
      "Subject \uD83D\uDD25",
      "cborg@codeforamerica.org",
      "This is a test \uD83D\uDC38"
    );
    return new ResponseEntity<>("Send test email", headers, HttpStatus.OK);
  }
}
