package formflow.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;

@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
@SpringBootTest(classes = {ThymeleafConfiguration.class}, properties = {"form-flow.design-system.name=honeycrisp"})
public class ThymeleafConfigurationUnloadedTest {

  @Autowired
  private TemplateEngine templateEngine;

  @Test
  void checkThatUSWDSTemplateResolverIsLoaded() {
    assertThat(templateEngine.getTemplateResolvers().size()).isEqualTo(1);
    var uswdsResolver = templateEngine.getTemplateResolvers().stream()
        .filter(x -> Objects.equals(x.getName(), "org.thymeleaf.templateresolver.ClassLoaderTemplateResolver")).findFirst();
    assertThat(uswdsResolver).isNotPresent();
  }
}
