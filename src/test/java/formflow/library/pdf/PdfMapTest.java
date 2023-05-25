package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PdfMapTest {

  private PdfMap pdfMap;

  @BeforeEach
  public void setup() {
    pdfMap = new PdfMap();
    Map<String, Object> inputFields = Map.of(
        "applicantFirstName", "PDF_APPLICANT_FIRST_NAME",
        "applicantLastName", "PDF_APPLICANT_LAST_NAME",
        "applicantDateOfBirth", "PDF_APPLICANT_BIRTH_DATE"
    );

    PdfMapSubflow pdfMapSubflowA = new PdfMapSubflow();
    pdfMapSubflowA.setSubflows(List.of("income", "household"));
    pdfMapSubflowA.setTotalIterations(5);
    pdfMapSubflowA.setFields(
        Map.of(
            "householdMemberFirstName", "PDF_HOUSEHOLD_MEMBER_FIRST_NAME",
            "householdMemberLastName", "PDF_HOUSEHOLD_MEMBER_LAST_NAME",
            "incomeJobAmount", "PDF_MEMBER_INCOME_JOB_AMOUNT",
            "incomeRetirementAmount", "PDF_MEMBER_INCOME_RETIREMENT_AMOUNT",
            "incomeTypes", Map.of(
                "incomeJob", "APPLICANT_HAS_JOB_INCOME",
                "incomeSelf", "APPLICANT_HAS_SELF_EMPLOYMENT_INCOME",
                "incomeUnemployment", "APPLICANT_HAS_UNEMPLOYMENT_INCOME",
                "incomeSocialSecurity", "APPLICANT_HAS_SOCIAL_SECURITY_INCOME"
            )
        )
    );

    pdfMap.setInputFields(inputFields);
    pdfMap.setSubflowInfo(Map.of("subflowA", pdfMapSubflowA));
  }

  @Test
  void GetSubflowFieldsAll() {
    Map<String, Object> subflowFields = pdfMap.getAllSubflowFields();
    PdfMapSubflow pdfMapSubflow = pdfMap.getSubflowInfo().get("subflowA");

    assertThat(subflowFields.containsKey("householdMemberFirstName_1")).isTrue();
    assertThat(subflowFields.containsKey("householdMemberFirstName_5")).isTrue();
    assertThat(subflowFields.containsKey("householdMemberFirstName_6")).isFalse();
    assertThat(subflowFields.size()).isEqualTo(pdfMapSubflow.getTotalIterations() * pdfMapSubflow.getFields().size());

    assertThat(subflowFields.containsKey("incomeTypes_1")).isFalse();
    assertThat(subflowFields.containsKey("incomeJob_1")).isTrue();
    assertThat(subflowFields.get("incomeJob_1")).isEqualTo("APPLICANT_HAS_JOB_INCOME_1");

    assertThat(subflowFields.get("incomeJobAmount_1")).isEqualTo("PDF_MEMBER_INCOME_JOB_AMOUNT_1");
  }
}
