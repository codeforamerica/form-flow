package formflow.library.submission.conditions;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FoundSubflowAddressSuggestion implements Condition {

    @Override
    public Boolean run(Submission submission, String uuid) {
        List<Map<String, Object>> subflowDataList = (List<Map<String, Object>>) submission.getInputData().get("testSubflow");
        Map<String, Object> theEntry = subflowDataList.get(0);
        return theEntry.containsKey("validationOnStreetAddress1_validated");
    }
}
