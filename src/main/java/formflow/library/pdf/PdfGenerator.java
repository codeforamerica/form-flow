package formflow.library.pdf;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PdfGenerator {
  public ApplicationFile generate(ApplicationFile blankFile, UUID id) {
    return new ApplicationFile(new byte[1], "fake");
  }

}
