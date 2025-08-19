package formflow.library.pdf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdfMapSubflow {

    List<String> subflows;
    int totalIterations;
    Map<String, Object> inputFields;

    /**
     * Returns all the inputFields associated with a subflow's iterations, expanded out, up to the totalIterations number defined
     * in the PdfMap yaml file.  All the inputFields represented by iteration data are represented in the returned Map.
     * <br>
     * Example Subflow data in the PDF file:
     * <pre>
     *   subflowInfo:
     *     householdAndIncome:
     *       totalIterations: 2
     *       inputFields:
     *         householdMemberFirstName: LEGAL_NAME_FIRST_MEMBER
     *         householdMemberLastName: LEGAL_NAME_LAST_MEMBER
     *         householdMemberRelationship: RELATIONSHIP_MEMBER
     *         incomeTypes:
     *           incomeJob: INCOME_HAS_JOB_MEMBER
     *           incomeSelf: INCOME_HAS_SELF_EMPLOYMENT_MEMBER
     *           incomeUnemployment: INCOME_HAS_UNEMPLOYMENT_MEMBER
     * </pre>
     * The above would generate the following map:
     * <pre>
     *   Map.of(
     *      "householdMemberFirstName_1" : "LEGAL_NAME_FIRST_MEMBER_1",
     *      "householdMemberLastName_1" : "LEGAL_NAME_LAST_MEMBER_1",
     *      "householdMemberRelationship_1" : "RELATIONSHIP_MEMBER_1",
     *      "incomeTypes_1" :
     *          incomeJob: INCOME_HAS_JOB_MEMBER
     *          incomeSelf: INCOME_HAS_SELF_EMPLOYMENT_MEMBER
     *          incomeUnemployment: INCOME_HAS_UNEMPLOYMENT_MEMBER
     * </pre>
     */
    public Map<String, Object> getFieldsForIterations() {
        Map<String, Object> iterationFields = new HashMap<>();
        AtomicInteger atomicInteger = new AtomicInteger(0);

        for (int index = 0; index < totalIterations; index++) {
            String suffix = "_" + (atomicInteger.get() + 1);

            inputFields.forEach((key, value) -> {
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
