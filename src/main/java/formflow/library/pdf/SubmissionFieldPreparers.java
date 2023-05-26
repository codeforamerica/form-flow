package formflow.library.pdf;

import formflow.library.config.ActionManager;
import formflow.library.data.Submission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubmissionFieldPreparers {

  private final List<DefaultSubmissionFieldPreparer> defaultPreparers;

  private final List<SubmissionFieldPreparer> customPreparers;

  private final PdfMapConfiguration pdfMapConfiguration;
  private final ActionManager actionManager;


  public SubmissionFieldPreparers(List<DefaultSubmissionFieldPreparer> defaultPreparers,
      List<SubmissionFieldPreparer> customPreparers,
      PdfMapConfiguration pdfMapConfiguration, ActionManager actionManager) {
    this.defaultPreparers = defaultPreparers;
    this.customPreparers = customPreparers;
    this.pdfMapConfiguration = pdfMapConfiguration;
    this.actionManager = actionManager;
  }

  /**
   * This method creates a list of SubmissionField objects which will be used to create the PdfField objects that will eventually
   * populate a given PDF. First it creates a single map of all input related data by flattening the subflow and input data
   * objects into a single map. Then, it iterates over each preparer and calls the prepareSubmissionFields passing the submission,
   * the flattened input data map and the PdfMap object from a given `pdf-map.yaml` file. First the default preparers are called
   * which will run the OneToOnePreparer, OneToManyPreparer and the DatabaseFieldPreparer. Then, it will call any custom preparers
   * that have been added to the application context by an implementer of the formflow library. The custom preparers are called
   * last so that any SubmissionField objects created by the default preparers can and will be overridden by the custom
   * preparers.
   *
   * @param submission
   * @return a list of SubmissionField objects for all fields in a given PdfMap from a `pdf-map.yaml` file.
   */
  public List<SubmissionField> prepareSubmissionFields(Submission submission) {
    PdfMap pdfMap = pdfMapConfiguration.getPdfMap(submission.getFlow());
    HashMap<String, SubmissionField> submissionFieldsMap = new HashMap<>();
    Map<String, PdfMapSubflow> subflowMap = pdfMap.getSubflowInfo();
    Map<String, Object> inputData = new HashMap<>();

    // set the inputData we want to hand to the preparers
    inputData.putAll(submission.getInputData());

    // TODO run data action - flow level action to manipulate data
    // manipulate data before preparing and mapping

    // now manage the subflow data
    //inputData.putAll(prepareSubflowData(submission, subflowMap));

    // now run preparers over all the fields.
    defaultPreparers.forEach(preparer -> {
      try {
        submissionFieldsMap.putAll(preparer.prepareSubmissionFields(submission, inputData, pdfMap));
      } catch (Exception e) {
        String preparerClassName = preparer.getClass().getSimpleName();
        log.error("There was an issue preparing submission data for " + preparerClassName, e);
      }
    });

    customPreparers.forEach(preparer -> {
      try {
        submissionFieldsMap.putAll(preparer.prepareSubmissionFields(submission, inputData, pdfMap));
      } catch (Exception e) {
        String preparerClassName = preparer.getClass().getSimpleName();
        log.error("There was an issue preparing submission data for " + preparerClassName, e);
      }
    });

    return submissionFieldsMap.values().stream().toList();
  }

  /**
   * This method takes the Submission and the subflow map from a given `pdf-map.yaml` file and iterates over each subflow in the
   * given subflow map to create an expanded map of data that can be used to populate the subflow related PDF fields. It also
   * takes into acount any action placed on a given subflow and will run the action to manipulate subflow data before created the
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
   * and the submission data:
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
   *
   * @param submission the submission
   * @param subflowMap the subflow mappings from the pdf-map.yaml file
   * @return an expanded map of string to object where each string key is an input field name and each object value is the value
   * of that input, either a single value string, or for checkbox inputs, a list of values.
   */
  public Map<String, Object> prepareSubflowData(Submission submission, Map<String, PdfMapSubflow> subflowMap) {

    List<Map<String, Object>> subflowDataList = new ArrayList<>();
    Map<String, Object> expandedSubflowData = new HashMap<>();

    if (subflowMap == null) {
      return expandedSubflowData;
    }

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
                  String.format(
                      "No subflow to work with specified for PDF subflow: %s. Unable to continue preparing data.",
                      pdfSubflowName
                  )
              );
            } else if (pdfSubflow.subflows.size() > 1) {
              String error = String.format(
                  "Error in PDF subflow %s configuration. No action was provided, but multiple subflows were indicated. " +
                      "There is no way to work with more than one subflow's data with out an Action to merge/collate the data.",
                  pdfSubflowName
              );
              log.error(error);
              throw new RuntimeException(error);
            }

            // there is only one subflow listed, so just bring its data forward
            if (submission.getInputData().containsKey(pdfSubflow.subflows.get(0))) {
              subflowDataList.addAll((List<Map<String, Object>>) submission.getInputData().get(pdfSubflow.subflows.get(0)));
            }
          }

          if (subflowDataList.size() > 0) {

            AtomicInteger atomInteger = new AtomicInteger(0);
            subflowDataList.forEach(iteration -> {
              // remove unnecessary fields
              iteration.remove("uuid");
              iteration.remove("iterationIsComplete");
              iteration.forEach((key, value) -> {

                // tack on suffix for field. "_%d" where %d is the iteration number
                if (key.endsWith("[]")) {
                  // don't update the inner values.
                  String newKey = key.replace("[]", "_" + (atomInteger.get() + 1) + "[]");
                  expandedSubflowData.put(newKey, value);
                } else {
                  expandedSubflowData.put(key + "_" + (atomInteger.get() + 1), value);
                }
              });
              atomInteger.incrementAndGet();
            });
          }
        }
    );

    return expandedSubflowData;
  }
}
