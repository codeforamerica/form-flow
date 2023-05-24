package formflow.library.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
                        "incomeJob", "PDF_MEMBER_INCOME_JOB",
                        "incomeRetirement", "PDF_MEMBER_INCOME_RETIREMENT"
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

        assertThat(subflowFields.get("incomeJob_1")).isEqualTo("PDF_MEMBER_INCOME_JOB_1");
    }
}
