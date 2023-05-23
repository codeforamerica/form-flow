package formflow.library.pdf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfMap {

  String flow;
  String pdf;
  Map<String, Object> inputFields;
  Map<String, Object> dbFields;
  Map<String, PdfMapSubflow> subflowInfo;

  public Map<String, Object> getAllSubflowFields() {
    Map<String, Object> subflowFields = new HashMap<>();

    AtomicInteger atomicInteger = new AtomicInteger(0);

    // TODO -- check this logic, it's probably not complete

    subflowInfo.forEach((subflowName, subflow) -> {
      Map<String, Object> fields = subflow.getFields();
      String suffix = "_" + atomicInteger.get() + 1;

      for (int index = 0; index < subflow.getTotalIterations(); index++) {
        Map<String, Object> singleIteration = new HashMap<>();

        fields.forEach((key, value) -> {
          // either return a List<Map<String, Object>> or
          // return a Map<String, Object>
          String newKey = addSuffix(key, suffix);

          if (value instanceof Map) {
            //Map<String, Object> val = (Map<String, Object>) fieldEntry.getValue();
            // run through all elements in the list it contains
            Map<String, Object> values = ((Map<String, Object>) value).entrySet().stream()
                .map(listEntry -> {
                  // don't change the key name here, it's not necessary
                  return (Map.entry(listEntry.getKey(), listEntry.getValue() + suffix));
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            //return (Map.entry(newKey, values));
            singleIteration.put(newKey, values);
          } else {
            //return (new AbstractMap.SimpleEntry(entrySet.getKey() + suffix, entrySet.getValue()));
            //return (Map.entry(newKey, fieldEntry.getValue() + suffix));
            singleIteration.put(newKey, value + suffix);
          }
        });
        subflowFields.putAll(singleIteration);
      }
    });
    return subflowFields;
  }

  private String addSuffix(String origString, String suffix) {
    int index = origString.lastIndexOf("[]");
    if (index != 0) {
      return origString.replaceFirst("\\[\\]$", suffix + "[]");
    } else {
      return origString + suffix;
    }


  }
}
