package formflow.library.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface that defines the abilities any File Virus Scanner should implement to work in this setup
 */
public interface FileVirusScanner {

  /**
   * This method will send the passed in `file` to a virus scanner service defined in the implementation and return a boolean
   * value indicating if the file contains a virus.
   *
   * @param file file to check for virus in
   * @return true if virus is found, false otherwise
   * @throws Exception
   */
  boolean virusDetected(MultipartFile file) throws Exception;
}
