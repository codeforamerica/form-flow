package formflow.library.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * This implementation of <code>FileVirusScanner</code> does nothing useful at all.
 * It logs the methods being called.
 */
@Service
@ConditionalOnProperty(name = "form-flow.uploads.virus-scanning.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoOpVirusScanner implements FileVirusScanner {

  /**
   * Default constructor.
   */
  public NoOpVirusScanner() {
  }

  @Override
  public boolean virusDetected(MultipartFile file) {
    log.info("Virus scanning disabled.  Returning false.");
    return false;
  }
}
