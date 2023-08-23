package formflow.library.file;

import formflow.library.exceptions.FileHasVirusException;
import formflow.library.exceptions.VirusScanException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@Slf4j
public class ClammitVirusScanner implements FileVirusScanner {

  private final String SCAN_PATH = "scan";
  private final String READY_PATH = "readyz";
  private final int TIMEOUT = 5000;

  @Value("${form-flow.uploads.virus-scanning.clammit-url}")
  String clammitUrl;


  @Override
  public Boolean doesFileHaveVirus(MultipartFile file) throws Exception {
    log.info("Clammit URL is " + clammitUrl);

    // TODO: we need a way to identify failure based on URL not being live

    String fullUrl = clammitUrl + "/" + SCAN_PATH;

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());

    WebClient client = getWebClient(fullUrl);

    try {
      boolean timeout = false;
      String responseBody = client
          .post()
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(body))
          .retrieve()
          // take this onStatus out if things are not working. It works okay with out it.
//          .onStatus(HttpStatus.I_AM_A_TEAPOT::equals,
//              response -> response.bodyToMono(String.class).map(FileHasVirusException::new))
          .bodyToMono(String.class)
          .timeout(Duration.ofMillis(TIMEOUT))
          .doOnError(TimeoutException.class, error -> {
            log.error("connection timed out when contacting clammit");
            throw new RuntimeException("Timeout connecting to the clammit service.");
          })
          .block();

      log.info("Clammit response: {}", responseBody);
      return responseBody.contains("OK");

    } catch (Exception e) {
      log.error("received exception from clammit server: {}", e.getMessage());
      throw new VirusScanException(
          String.format("Clammit service unable to process request for file. It returned: %s", e.getMessage()));
    }
  }

  // added in case we want to do a check in the above function to see that the end point is live. That would
  // allow us to send an exception if not.  But it would be two calls, versus one.
  // Or we could/should have a constructor that checks if the endpoint is live.  Seems like we should check.
  public Boolean isReady() {
    String fullUrl = clammitUrl + "/" + READY_PATH;
    WebClient client = getWebClient(fullUrl);
    HttpResponse httpResponse = client
        .get()
        .retrieve()
        .bodyToMono(HttpResponse.class)
        .block();

    return httpResponse.statusCode() == 200;
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