package formflow.library.pdf;

import formflow.library.config.ActionManager;
import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubmissionSubflowFieldPreparer implements DefaultSubmissionFieldPreparer {

  @Autowired
  ActionManager actionManager;

  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, Map<String, Object> data, PdfMap pdfMap) {
    Map<String, Object> fieldMap = pdfMap.getAllFields();
    Map<String, SubmissionField> preppedFields = new HashMap<>();
    List<Map<String, Object>> subflowDataList = new ArrayList<>();
    Map<String, PdfMapSubflow> subflowMap = pdfMap.getSubflowInfo();

    subflowMap.forEach((pdfSubflowName, pdfSubflow) -> {
      // run action on data
      if (pdfSubflow.dataAction != null) {
        subflowDataList.addAll(actionManager.getAction(pdfSubflow.dataAction).runSubflowAction(submission, pdfSubflow));
      } else {
        // if there are no subflows specified or if there is no action supplied, we can't possibly know how
        // to combine more than one subflow's data and work with it successfully.  Send an error.
        if (pdfSubflow.subflows == null) {
          log.error("No subflows provided for PDF Subflow: " + pdfSubflowName);
          throw new RuntimeException(
              String.format("No subflow to work with specified for PDF subflow: %s. Unable to continue preparing data.",
                  pdfSubflowName));
        } else if (pdfSubflow.subflows.size() > 1) {
          String error = String.format(
              "Error in PDF subflow %s configuration. No action was provided, but multiple subflows were indicated. "
                  + "There is no way to work with more than one subflow's data with out an Action to merge/collate the data.",
              pdfSubflowName);
          log.error(error);
          throw new RuntimeException(error);
        }

        // there is only one subflow listed, so just bring its data forward
        if (submission.getInputData().containsKey(pdfSubflow.subflows.get(0))) {
          subflowDataList.addAll((List<Map<String, Object>>) submission.getInputData().get(pdfSubflow.subflows.get(0)));
        }
      }

      if (subflowDataList.size() > 0) {

        AtomicInteger atomInteger = new AtomicInteger(1);
        subflowDataList.forEach(iteration -> {
          // remove unnecessary fields
          iteration.remove("uuid");
          iteration.remove("iterationIsComplete");
          iteration.forEach((key, value) -> {

            // tack on suffix for field. "_%d" where %d is the iteration number
            if (key.endsWith("[]")) {
              // don't update the inner values.
              String newKey = key.replace("[]", "_" + atomInteger.get() + "[]");
              preppedFields.put(newKey, new CheckboxField(key.replace("[]", ""),
                  (List<String>) value, atomInteger.get()));
              //expandedSubflowData.put(newKey, value);
            } else {
              //expandedSubflowData.put(key + "_" + (atomInteger.get() + 1), value);
              preppedFields.put(key + "_" + atomInteger.get(), new SingleField(key, value.toString(), atomInteger.get()));
            }
          });
          atomInteger.incrementAndGet();
        });
      }
    });
    return preppedFields;
  }
}
