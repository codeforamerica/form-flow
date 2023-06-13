package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml", "form-flow.error.show-stack-trace=false"}, webEnvironment = RANDOM_PORT)
public class ShowCustomErrorPageTest extends AbstractBasePageTest {

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "flow/inputs/asdf";
    super.setUp();
  }

  @Test
  void showCustomErrorPageToHideInternalErrorWhenPropertyIsDisabled() {
    assertThat(testPage.getBody()).contains("We're sorry, but something went wrong. Please try again later.");
  }

}
