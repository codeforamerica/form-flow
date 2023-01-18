package formflow.library.inputs;

import javax.persistence.Transient;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class UploadFlow {

  // Ignore csrf for validation
  @Transient
  String _csrf;
  String uploadTest;
}
