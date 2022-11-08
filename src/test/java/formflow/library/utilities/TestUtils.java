package formflow.library.utilities;

import formflow.library.data.Submission;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class TestUtils {

  public static void resetSubmission(Submission submission) {
    submission = new Submission();
    submission.setId(null);
    submission.setInputData(new HashMap<>());
    submission.setFlow(null);
  }

  public static Path getAbsoluteFilepath(String resourceFilename) {
    return Paths.get(getAbsoluteFilepathString(resourceFilename));
  }

  public static String getAbsoluteFilepathString(String resourceFilename) {
    URL resource = TestUtils.class.getClassLoader().getResource(resourceFilename);
    if (resource != null) {
      return (new File(resource.getFile())).getAbsolutePath();
    }
    return "";
  }

  public static byte[] getFileContentsAsByteArray(String filename) throws IOException {
    return Files.readAllBytes(getAbsoluteFilepath(filename));
  }
}
