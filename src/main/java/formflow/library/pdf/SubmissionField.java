package formflow.library.pdf;

public abstract class SubmissionField {

  public Integer iteration = null;

  public String name = null;

  SubmissionField(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getNameWithIteration(String name) {
    return iteration != null ? name + "_" + iteration : name;
  }
}
