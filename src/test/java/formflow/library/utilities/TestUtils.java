package formflow.library.utilities;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

public class TestUtils {

  public static String getAbsoluteFilepathString(String resourceFilename) {
    URL resource = TestUtils.class.getClassLoader().getResource(resourceFilename);
    if (resource != null) {
      return (new File(resource.getFile())).getAbsolutePath();
    }
    return "";
  }

  public static OffsetDateTime makeOffsetDateTime(String isoDate) {
    return LocalDate.parse(isoDate).atTime(OffsetTime.parse("00:00-08:00"));
  }
}
