package formflow.library.config;

import formflow.library.data.Submission;

import java.util.ArrayList;
import java.util.Map;

/**
 * TODO - this my not belong in the form flow lib?
 */
public class SubmissionActions {
    public static void clearIncomeAmountsBeforeSaving(Submission submission, String uuid) {
        //grab the current household members incometypes
        var entryByUuid = Submission.getSubflowEntryByUuid("income", uuid, submission);
        var incomeAmounts = entryByUuid.entrySet().stream()
                .filter(e -> e.getKey().matches(".*Amount$")).map(Map.Entry::getKey).toList();

        var incomeTypesArray = (ArrayList<String>) entryByUuid.get("incomeTypes[]");

        incomeAmounts.stream().forEach(incomeAmount -> {
            if (!incomeTypesArray.contains(incomeAmount.replace("Amount", ""))) {
                entryByUuid.remove(incomeAmount);
            }
        });
    }
}
