package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfFieldMapperTest {

    PdfMap pdfMapConfiguration;

    @BeforeEach
    void setUp() {
        pdfMapConfiguration = new PdfMap();
        pdfMapConfiguration.setFlow("testFlow");
    }

    @Test
    public void shouldMapSingleFieldsToPdfFields() {
        String inputName = "testInput";
        String pdfFieldName = "TEST_FIELD";
        String stringValue = "testValue";

        pdfMapConfiguration.setInputFields(Map.of(inputName, pdfFieldName));

        SingleField singleField = new SingleField(inputName, stringValue, null);

        PdfFieldMapper pdfFieldMapper = new PdfFieldMapper(List.of(pdfMapConfiguration));
        List<PdfField> fields = pdfFieldMapper.map(List.of(singleField), "testFlow");

        assertThat(fields).contains(new PdfField(pdfFieldName, stringValue));
    }

    @Test
    void shouldCheckboxFieldsToMultiplePdfFieldsWithYesForValues() {
        String inputName = "checkboxInput";
        List<String> value = List.of("value1", "value2");

        CheckboxField checkboxField = new CheckboxField(inputName,
                value, null);

        pdfMapConfiguration.setInputFields(Map.of(inputName,
                Map.of(
                        "value1", "PDF_FIELD_NAME_1",
                        "value2", "PDF_FIELD_NAME_2"
                )));

        PdfFieldMapper pdfFieldMapper = new PdfFieldMapper(List.of(pdfMapConfiguration));
        List<PdfField> fields = pdfFieldMapper.map(List.of(checkboxField), "testFlow");

        assertThat(fields).contains(
                new PdfField("PDF_FIELD_NAME_1", "Yes"),
                new PdfField("PDF_FIELD_NAME_2", "Yes")
        );
    }

    @Test
    void shouldCatchErrorsIfFieldCantBeFoundAndContinue() {
        pdfMapConfiguration.setInputFields(Map.of(
                "testInput", "TEST_FIELD"
        ));

        PdfFieldMapper pdfFieldMapper = new PdfFieldMapper(List.of(pdfMapConfiguration));
        List<PdfField> fields = pdfFieldMapper.map(List.of(
                new SingleField("testInput", "testValue", null),
                new SingleField("fieldThatIsNotInPdfMap", "someValue", null)
        ), "testFlow");

        assertThat(fields).contains(new PdfField("TEST_FIELD", "testValue"));
    }
}
