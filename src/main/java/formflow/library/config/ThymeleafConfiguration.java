package formflow.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Provides configuration for the Thymeleaf templates, besides the default.
 */
@Configuration
@Slf4j
public class ThymeleafConfiguration {

  /**
   * Creates a new ClassLoaderTemplateResolver to be able to resolve templates in the {@code cfa-uswds-templates/} directory.
   *
   * <p>
   * The resolver will be configured to look in the {@code cfa-uswds-templates} directory for {@code .html} files. The assumed
   * encoding will be {@code UTF-8}.  The templates will not be cached.
   * </p>
   *
   * @return a new resolver configured to work with library uswds templates.
   */
  @Bean
  @ConditionalOnProperty(name = "form-flow.design-system.name", havingValue = "cfa-uswds")
  public ClassLoaderTemplateResolver cfaUswdsTemplateResolver() {
    log.info("Template resolution has been set to include the path `cfa-uswds-templates/`.");
    ClassLoaderTemplateResolver secondaryTemplateResolver = new ClassLoaderTemplateResolver();
    secondaryTemplateResolver.setPrefix("cfa-uswds-templates/");
    secondaryTemplateResolver.setSuffix(".html");
    secondaryTemplateResolver.setTemplateMode(TemplateMode.HTML);
    secondaryTemplateResolver.setCharacterEncoding("UTF-8");
    secondaryTemplateResolver.setOrder(0);
    secondaryTemplateResolver.setCheckExistence(true);
    secondaryTemplateResolver.setCacheable(false);

    return secondaryTemplateResolver;
  }
}
