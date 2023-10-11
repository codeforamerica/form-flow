package formflow.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@SpringBootTest(classes = {ThymeleafConfiguration.class}, properties = {"form-flow.design-system.name=cfa-uswds"})
@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
class ThymeleafConfigurationTest {

  @Autowired
  private ThymeleafConfiguration thymeleafConfiguration;

  @Autowired
  private TemplateEngine templateEngine;

  @Test
  void checkThatUSWDSTemplateResolverIsLoaded() {
    System.out.println(templateEngine.getTemplateResolvers());
    assertThat(templateEngine.getTemplateResolvers().size()).isEqualTo(2);

    assertThat(thymeleafConfiguration).isEqualTo("test");
  }

}
