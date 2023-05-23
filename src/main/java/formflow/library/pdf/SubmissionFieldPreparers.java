package formflow.library.pdf;

import formflow.library.config.ActionManager;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LoggingInitializationContext;
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

  public List<SubmissionField> prepareSubmissionFields(Submission submission) {
    // do note that we are going to get the submission and then change it's inputData
    // drastically over the course of this method. That's why we want our own copy.  We will
    // not save it back at all.
    PdfMap pdfMap = pdfMapConfiguration.getPdfMap(submission.getFlow());
    HashMap<String, SubmissionField> submissionFieldsMap = new HashMap<>();
    Map<String, PdfMapSubflow> subflowMap = pdfMap.getSubflowInfo();
    Map<String, Object> inputData = new HashMap<>();

    // set the inputData we want to hand to the preparers
    inputData.putAll(submission.getInputData());

    // now manage the subflow data
    inputData.putAll(prepareSubflowData(submission, subflowMap));

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

  public Map<String, Object> prepareSubflowData(Submission submission, Map<String, PdfMapSubflow> subflowMap) {

    List<Map<String, Object>> subflowDataList = new ArrayList<>();
    Map<String, Object> inputData = new HashMap<>();

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
                inputData.put(key + "_" + (atomInteger.get() + 1), value);
              });
              atomInteger.incrementAndGet();
            });
            // put it in the submissionFieldsMap
            //subflowDataList.forEach(submission.getInputData()::putAll);
          }
        }
    );

    return inputData;


  }
}
