package formflow.library.file;


import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
public class ClammitVirusScanner implements FileVirusScanner {

  @Value("${form-flow.uploads.virus-scanning.clammit-url}")
  String clammitUrl;


  @Override
  public boolean virusDetected(MultipartFile file) throws Exception {
    log.info("Clammit URL is " + clammitUrl);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());

    WebClient client = WebClient.builder().baseUrl(clammitUrl).build();

    try {
      int TIMEOUT = 5000;
      String responseBody = client
          .post()
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(body))
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofMillis(TIMEOUT))
          .onErrorResume(e -> {
            if (e instanceof TimeoutException) {
              throw webClientException(408, "WebClient timed out while attempting to reach " + clammitUrl);
            }
            if (e instanceof WebClientResponseException
                && ((WebClientResponseException) e).getStatusCode() != HttpStatus.I_AM_A_TEAPOT) {
              throw webClientException(((WebClientResponseException) e).getStatusCode().value(),
                  "There was a problem when the web client attempted to get a response from " + clammitUrl);
            }
            throw webClientException(500,
                "There was a problem when the web client attempted to get a response from " + clammitUrl);
          })
          .block();
      log.info("Clammit response: {}", responseBody);
      return false;
    } catch (WebClientResponseException e) {
      if (e.getStatusCode() == HttpStatus.I_AM_A_TEAPOT) {
        log.error("The uploaded file contains a virus.");
        return true;
      }
      if (e.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
        throw new TimeoutException(e.getMessage());
      }
      log.error("Received unexpected exception from clammit server: {}", e.getMessage());
      throw webClientException(500, e.getMessage());
    }
  }

  private WebClientResponseException webClientException(int status, String message) {
    return new WebClientResponseException(status, message, null, null, null);
  }
}