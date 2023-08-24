package formflow.library.file;


import java.net.http.HttpResponse;
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

  private final int TIMEOUT = 5000;

  @Value("${form-flow.uploads.virus-scanning.clammit-url}")
  String clammitUrl;


  @Override
  public Boolean doesFileHaveVirus(MultipartFile file) throws Exception {
    log.info("Clammit URL is " + clammitUrl);


    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());

    WebClient client = getWebClient(clammitUrl);

    try {
      String responseBody = client
          .post()
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(body))
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofMillis(TIMEOUT))
          .doOnError(TimeoutException.class, error -> {
            log.error("connection timed out when contacting clammit");
            throw new RuntimeException("Timeout connecting to the clammit service.");
          })
          .block();
      log.info("Clammit response: {}", responseBody);
      return !responseBody.contains("No virus found");
    } catch (WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.I_AM_A_TEAPOT)) {
        log.error("The uploaded file contains a virus.");
        return true;
      } else {
        log.error("Received unexpected exception from clammit server: {}", e.getMessage());
        throw new RuntimeException(e.getMessage());
      }
    } catch (Exception e) {
      // something else failed within the service
      log.error("Received unexpected exception from clammit server: {}", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }


  private WebClient getWebClient(String url) {
   /* final TcpClient tcpClient = TcpClient
        .create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT)
        .doOnConnected(connection -> {
          connection.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS));
          connection.addHandlerLast(new WriteTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS));
        });
*/

    return WebClient.builder()
        .baseUrl(url)
        //       .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
        .build();
  }
}