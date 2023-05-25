package formflow.library.pdf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfMapSubflow {

  List<String> subflows;
  int totalIterations;
  String dataAction;
  Map<String, Object> fields;

  public Map<String, Object> getFieldsForIterations() {
    Map<String, Object> iterationFields = new HashMap<>();
    AtomicInteger atomicInteger = new AtomicInteger(0);

    for (int index = 0; index < totalIterations; index++) {
      String suffix = "_" + (atomicInteger.get() + 1);

      fields.forEach((key, value) -> {
        String newKey = key + suffix;

        if (value instanceof Map) {
          Map<String, Object> values = ((Map<String, Object>) value).entrySet().stream()
              .map(listEntry -> {
                // don't change the key name here, it's not necessary
                return (Map.entry(listEntry.getKey(), listEntry.getValue() + suffix));
              }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

          iterationFields.put(newKey, values);
        } else {
          iterationFields.put(newKey, value + suffix);
        }
      });
      atomicInteger.getAndIncrement();
    }
    return iterationFields;
  }
}
