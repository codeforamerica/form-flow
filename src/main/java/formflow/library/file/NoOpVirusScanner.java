package formflow.library.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "form-flow.uploads.virus-scanning.enabled", havingValue = "false")
public class NoOpVirusScanner implements FileVirusScanner {

  @Override
  public boolean virusDetected(MultipartFile file) {
    return false;
  }
}
