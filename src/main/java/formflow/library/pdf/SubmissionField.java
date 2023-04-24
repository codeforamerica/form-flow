package formflow.library.pdf;

public interface SubmissionField {
  Integer iteration = null;

  default String getNameWithIteration(String name) {
    return iteration != null ? name + "_" + iteration : name;
  }
}
