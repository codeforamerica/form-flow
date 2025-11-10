package formflow.library.file;


import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Clammit Virus Scanner An implementation of the FileVirusScanner interface that will use the
 * <a href="https://github.com/codeforamerica/form-flow/tree/create-clammit-av-service">Clammit Virus Scanner Server</a> to check
 * files for viruses.
 * <p>
 * The Clammit Virus Scanner Server itself is set up independently of this code base.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "form-flow.uploads.virus-scanning.enabled", havingValue = "true")
public class ClammitVirusScanner implements FileVirusScanner {

    String clammitUrl;
    int timeout;

    /**
     * Clammit Virus Scanner An implementation of the FileVirusScanner interface that will use the
     * <a href="https://github.com/codeforamerica/form-flow/tree/create-clammit-av-service">Clammit Virus Scanner Server</a> to
     * check files for viruses.
     * <p>
     * The Clammit Virus Scanner Server itself is set up independently of this code base.     * @param clammitUrl Clammit AV
     * service url
     *
     * @param clammitUrl Url for the clammit av service
     * @param timeout    Timeout allowed for service call
     */
    public ClammitVirusScanner(
            @Value("${form-flow.uploads.virus-scanning.service-url}") String clammitUrl,
            @Value("${form-flow.uploads.virus-scanning.timeout:5000}") int timeout) {
        log.info("Clammit Virus Scanner created!");
        this.clammitUrl = clammitUrl;
        this.timeout = timeout;
    }

    @Override
    public boolean virusDetected(MultipartFile file) throws TimeoutException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        WebClient client = WebClient.builder().baseUrl(clammitUrl).build();

        try {
            String responseBody = client
                    .post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .onErrorResume(e -> {
                        if (e instanceof TimeoutException) {
                            throw createWebClientResponseException(408,
                                    "WebClient timed out while attempting to reach " + clammitUrl);
                        }
                        if (e instanceof WebClientResponseException
                                && ((WebClientResponseException) e).getStatusCode() == HttpStatus.I_AM_A_TEAPOT) {
                            throw createWebClientResponseException(418, "The uploaded file has a virus.");
                        }
                        if (e instanceof WebClientResponseException
                                && ((WebClientResponseException) e).getStatusCode() != HttpStatus.I_AM_A_TEAPOT) {
                            throw createWebClientResponseException(((WebClientResponseException) e).getStatusCode().value(),
                                    "There was a problem when the web client attempted to get a response from " + clammitUrl);
                        }
                        throw createWebClientResponseException(500,
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
            log.error("Received unexpected exception from clammit server", e);
            throw createWebClientResponseException(500, e.getMessage());
        }
    }

    private WebClientResponseException createWebClientResponseException(int status, String message) {
        return new WebClientResponseException(status, message, null, null, null);
    }
}