package formflow.library.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface that defines the abilities any File Virus Scanner should implement to work in this setup
 */
public interface FileVirusScanner {

  boolean virusDetected(MultipartFile file) throws Exception;
}
