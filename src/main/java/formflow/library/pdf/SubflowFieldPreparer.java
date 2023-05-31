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

/**
 * This preparer takes the Submission and the subflow map from a given `pdf-map.yaml` file and iterates over each subflow in the
 * given subflow map to create an expanded map of data that can be used to populate the subflow related PDF fields. It also takes
 * into account any action placed on a given subflow and will run the action to manipulate subflow data before created the
 * aforementioned expanded map. For example given the following subflow map:
 * <pre>
 * income:
 *   totalIterations: 3
 *   subflows:
 *     - income
 *   fields:
 *     exampleInput: EXAMPLE_PDF_FIELD
 *     otherExampleInput: ANOTHER_EXAMPLE_PDF_FIELD
 *     exampleCheckboxField:
 *       firstValue: FIRST_VALUE_PDF_FIELD
 *       secondValue: SECOND_VALUE_PDF_FIELD
 * </pre>
 * <p>
 * and the Submission `inputData`:
 * <pre>
 *   {
 *     income: [
 *       {
 *         exampleInput: "exampleInput value",
 *         otherExampleInput: "otherExampleInput value"
 *         exampleCheckboxField: ["firstValue", "secondValue"]
 *       },
 *       {
 *         exampleInput: "exampleInput value 2",
 *         otherExampleInput: "otherExampleInput value 2"
 *         exampleCheckboxField: ["firstValue", "secondValue"]
 *       }
 *     ]
 *   }
 * </pre>
 * The resulting expanded map would be:
 * <pre>
 *   {
 *     exampleInput_1: "exampleInput value",
 *     exampleInput_2: "exampleInput value 2",
 *     otherExampleInput_1: "otherExampleInput value",
 *     otherExampleInput_2: "otherExampleInput value 2"
 *     exampleCheckboxField_1[]: ["firstValue", "secondValue"],
 *     exampleCheckboxField_2[]: ["firstValue", "secondValue"]
 *   }
 * </pre>
 * <p>
 * This allows us to add the expanded subflow map to the submission's inputData ultimately creating one big flattened map of all
 * data from top level inputs and from subflows.
 */
@Slf4j
@Component
public class SubflowFieldPreparer implements DefaultSubmissionFieldPreparer {

  @Autowired
  ActionManager actionManager;

  /**
   * @param submission the submission
   * @param data       the data to act upon
   * @param pdfMap     the field mappings from the pdf-map.yaml file
   * @return a map of field name to SubmissionField
   */
  public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, Map<String, Object> data, PdfMap pdfMap) {
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

          // only prepare as many as PDF can hold.
          if (atomInteger.get() > pdfSubflow.getTotalIterations()) {
            return;
          }

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

  /**
   * Set the ActionManager explicitly. Generally the ActionManager is AutoWired in, but in the event that it can't be, or we want
   * to override it, this provides the ability to do that.
   *
   * @param actionManager ActionManager to use
   */
  public void setActionManager(ActionManager actionManager) {
    this.actionManager = actionManager;
  }
}
