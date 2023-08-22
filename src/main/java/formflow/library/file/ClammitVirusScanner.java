package formflow.library.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpHead;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class ClammitVirusScanner implements FileVirusScanner {
  
  @Value("${form-flow.uploads.virus-scanning.clammit-url}")
  String clammitUrl;
  
  @Override
  public Boolean doesFileHaveVirus(MultipartFile file) {
    log.info("Clammit URL is " + clammitUrl);
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file.getResource());
    String response = restTemplate.postForObject(clammitUrl, body, String.class);
    log.info("Clammit response: {}", response);

    return response.contains("OK");
  }
}
