package formflow.library.pdf;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public abstract class SubmissionField {

  @ToString.Include
  @EqualsAndHashCode.Include
  public String name = null;

  @ToString.Include
  @EqualsAndHashCode.Include
  public Integer iteration = null;

  public String getNameWithIteration() {
    return iteration != null ? name + "_" + iteration : name;
  }
}
