package formflow.library.pdf;

import lombok.ToString;
import lombok.Value;

@Value
@ToString(exclude = {"fileBytes"})
public class ApplicationFile {

  byte[] fileBytes;
  String fileName;
}
