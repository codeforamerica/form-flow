package formflow.library.inputs;

import formflow.library.data.FlowInputs;
import formflow.library.data.annotations.Money;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestRelatedSubflows extends FlowInputs {
    
    private String hasHousehold;
    private String householdMemberFirstName;
    private String householdMemberLastName;
    private ArrayList<String> incomeTypes;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeJobAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeSelfAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeUnemploymentAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeSocialSecurityAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeRetirementAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeChildOrSpousalSupportAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomePensionAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeInvestmentAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeCapitalGainsAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeRentalOrRoyaltyAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeFarmOrFishAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeAlimonyAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeTaxableScholarshipAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeCancelledDebtAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeCourtAwardsAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeGamblingAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeJuryDutyPayAmount;
    @NotBlank(message = "{income-amounts.must-select-one}")
    @Money(message = "{income-amounts.must-be-dollars-cents}")
    private String incomeOtherAmount;
}
