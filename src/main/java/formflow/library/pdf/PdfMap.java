package formflow.library.pdf;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

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

    subflowInfo.forEach((subflowName, subflow) -> {
      Map<String, Object> fields = subflow.getFields();
      String suffix = "_" + atomicInteger.get() + 1;

      for (int index = 0; index < subflow.getTotalIterations(); index++) {
        subflowFields.putAll(
            fields.entrySet().stream()
                .map(entrySet -> {
                  // either return a List<Map<String, Object>> or
                  // return a Map<String, Object>
                  String newKey = addSuffix(entrySet.getKey(), suffix);

                  TODO use instanceOf not [] because those are not there in PDF map.

                  if (newKey.endsWith("[]")) {
                    Map<String, Object> val =(Map<String, Object>)entrySet.getValue();
                    // run through all elements in the list it contains
                    Map<String, Object> values = val.entrySet().stream()
                        .map((inputName, pdfName) -> {
                          return(Map.entry(inputName, pdfName + suffix);
                        });

                    return(Map.entry(newKey, values));
                  } else {
                    //return (new AbstractMap.SimpleEntry(entrySet.getKey() + suffix, entrySet.getValue()));
                    return (Map.entry(newKey, val + suffix);
                  }
                })
        );
      }

      allFields.putAll(updatedFields);
      wrapper.index++;
      if (wrapper.index > subflow.totalIterations) {
        break;
      }

    });

    return allFields;
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
