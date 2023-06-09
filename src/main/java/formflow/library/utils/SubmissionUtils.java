package formflow.library.utils;

import formflow.library.data.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmissionUtils {

  public static Submission copySubmission(Submission origSubmission) {
    Submission newSubmission = new Submission();
    newSubmission.setUrlParams(new HashMap<>(origSubmission.getUrlParams()));

    newSubmission.setFlow(origSubmission.getFlow());
    newSubmission.setCreatedAt(origSubmission.getCreatedAt());
    newSubmission.setSubmittedAt(origSubmission.getSubmittedAt());
    newSubmission.setId(origSubmission.getId());

    // deep copy the subflows and any lists
    newSubmission.setInputData(copyMap(origSubmission.getInputData()));
    return newSubmission;
  }


  private static Map<String, Object> copyMap(Map<String, Object> origMap) {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : origMap.entrySet()) {
      if (entry.getValue() instanceof List) {
        List data = (List) entry.getValue();
        if (data.get(0) instanceof String) {
          result.put(entry.getKey(), new ArrayList<>(data));
        } else if (data.get(0) instanceof Map) {
          List<Map> newList = new ArrayList<>();
          for (Map element : (List<Map>) data) {
            newList.add(copyMap(element));
          }
          result.put(entry.getKey(), newList);
        } else {
          result.put(entry.getKey(), entry.getValue());
        }
      } else {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }
}
