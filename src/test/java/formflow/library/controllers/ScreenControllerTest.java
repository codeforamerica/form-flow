package formflow.library.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import formflow.library.ScreenController;
import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import formflow.library.utilities.AbstractMockMvcTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
public class ScreenControllerTest extends AbstractMockMvcTest {

  @MockBean
  private AddressValidationService addressValidationService;

  @MockBean
  private SubmissionRepositoryService submissionRepositoryService;



  @Override
  @BeforeEach
  public void setUp() throws Exception {
    UUID submissionUUID = UUID.randomUUID();
    submission = Submission.builder().id(submissionUUID).urlParams(new HashMap<>()).inputData(new HashMap<>()).build();
    when(submissionRepositoryService.findOrCreate(any())).thenReturn(submission);
    super.setUp();
  }

  @Nested
  public class UrlParameterPersistence {

    @Test
    public void passedUrlParametersShouldBeSaved() throws Exception {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("lang", "en");
      getWithQueryParam("test", "lang", "en");
      assert(submission.getUrlParams().equals(queryParams));
    }
  }

  @Nested
  public class SubflowParameters {
    @Test
    public void modelIncludesCurrentSubflowItem() throws Exception {
      HashMap<String, String> subflowItem = new HashMap<>();
      subflowItem.put("uuid", "aaa-bbb-ccc");
      subflowItem.put("firstName", "foo bar baz");

      submission.setInputData(Map.of("testSubflow", List.of(subflowItem)));
      getPageExpectingSuccess("subflowAddItem/aaa-bbb-ccc");
    }
  }

  @Nested
  public class AddressValidation {

    @Test
    public void addressValidationShouldRunAfterFieldValidation() throws Exception {
      var params = new HashMap<String, List<String>>();
      params.put("_validatevalidationOn", List.of("true"));
      params.put("validationOnStreetAddress1", List.of("880 N 8th St"));
      params.put("validationOnStreetAddress2", List.of("Apt 2"));
      // City is required
      params.put("validationOnCity", List.of(""));
      params.put("validationOnState", List.of("NM"));
      params.put("validationOnZipCode", List.of("88201"));

      postExpectingFailure("testAddressValidation", params);
      verify(addressValidationService, times(0)).validate(any());
    }

    @Test
    public void addressValidationShouldOnlyRunWhenSetToTrue() throws Exception {
      when(addressValidationService.validate(any())).thenReturn(Map.of(
          "validationOn",
          new ValidatedAddress("validatedStreetAddress",
              "validatedAptNumber",
              "validatedCity",
              "validatedState",
              "validatedZipCode-1234")
      ));

      var params = new HashMap<String, List<String>>();
      params.put("_validatevalidationOff", List.of("false"));
      params.put("validationOffStreetAddress1", List.of("110 N 6th St"));
      params.put("validationOffStreetAddress2", List.of("Apt 1"));
      params.put("validationOffCity", List.of("Roswell"));
      params.put("validationOffState", List.of("NM"));
      params.put("validationOffZipCode", List.of("88201"));
      params.put("_validatevalidationOn", List.of("true"));
      params.put("validationOnStreetAddress1", List.of("880 N 8th St"));
      params.put("validationOnStreetAddress2", List.of("Apt 2"));
      params.put("validationOnCity", List.of("Roswell"));
      params.put("validationOnState", List.of("NM"));
      params.put("validationOnZipCode", List.of("88201"));

      postExpectingSuccess("testAddressValidation", params);

      verify(addressValidationService, times(1)).validate(any());
    }
  }
}
