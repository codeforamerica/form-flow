package formflow.library.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import formflow.library.UploadController;
import formflow.library.utilities.AbstractMockMvcTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {"pagesConfig=pages-config/test-pages-controller.yaml"})
public class UploadControllerTest extends AbstractMockMvcTest {

  // mock S endpoint
  // mock database
  // actual upload controller
  private MockMvc mockMvc;

  @Autowired
  private UploadController uploadController;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(uploadController).build();
    super.setUp();
  }

  @Test
  public void testFileUpload() throws Exception {
    // uploadController.upload(...);
    // RUN asserts
    MockHttpServletRequestBuilder request = post("/submit")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .param("foo[]", "some value");
    mockMvc.perform(request).andExpect(redirectedUrl("/pages/secondPage/navigation"));
    assertThat(s3Repository)
  }


}
