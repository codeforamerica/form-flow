package formflow.library.file;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
  public Boolean doesFileHaveVirus(MultipartFile file) {
    log.info("Clammit URL is " + clammitUrl);

    // TODO: we need a way to identify failure based on URL not being live

    String fullUrl = clammitUrl + "/" + SCAN_PATH;

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());

    WebClient client = getWebClient(fullUrl);

    String response = client
        .post()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(body))
        .retrieve()
        .bodyToMono(String.class)
        .block();

    log.info("Clammit response: {}", response);

    return response.contains("OK");
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
    final var tcpClient = TcpClient
        .create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT)
        .doOnConnected(connection -> {
          connection.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS));
          connection.addHandlerLast(new WriteTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS));
        });

    WebClient client = WebClient.builder()
        .baseUrl(clammitUrl + "/" + SCAN_PATH)
        .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
        .build();

    return client;
  }
}
