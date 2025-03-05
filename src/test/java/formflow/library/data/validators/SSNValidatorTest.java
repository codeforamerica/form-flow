package formflow.library.data.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
class SSNValidatorTest {

    @Autowired
    private SSNValidator ssnValidator;

    //valid SSNs do not begin with 000, 666, or 900-999, do not have 00 in the group position (middle two digits), or end in 0000.”
    @ValueSource(strings = {"123-12-1234", "782-98-5200", "665-01-0001", "899-10-0030"})
    @ParameterizedTest
    void validSSNShouldReturnTrue(String value) {
        assertThat(ssnValidator.isValid(value, null)).isTrue();
    }

    @ValueSource(strings = {"000-12-1234", "666-98-5200", "900-01-0001", "934-10-0030", "123-00-0030", "123-10-000"})
    @ParameterizedTest
    void invalidSSNShouldReturnFalse(String value) {
        assertThat(ssnValidator.isValid(value, null)).isFalse();
    }
}