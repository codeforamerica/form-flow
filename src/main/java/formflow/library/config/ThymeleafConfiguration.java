package formflow.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Provides configuration for the Thymeleaf templates.
 */
@Configuration
public class ThymeleafConfiguration {
  /**
   * Creates a new SpringResourceTemplateResolver and configures it so it knows where to locate the templates and
   * how to read them.
   *
   * <p>
   *     The resolver will be configured to look in the {@code templates/} directory for {@code .html} files.
   *     The assumed encoding will be {@code UTF-8}.  The templates will not be cached.
   * </p>
   *
   * @return a new resolver configured to work with templates
   */
  @Bean
  public SpringResourceTemplateResolver secondaryTemplateResolver() {
    SpringResourceTemplateResolver secondaryTemplateResolver = new SpringResourceTemplateResolver();
    secondaryTemplateResolver.setPrefix("templates/");
    secondaryTemplateResolver.setSuffix(".html");
    secondaryTemplateResolver.setTemplateMode(TemplateMode.HTML);
    secondaryTemplateResolver.setCharacterEncoding("UTF-8");
    secondaryTemplateResolver.setOrder(1);
    secondaryTemplateResolver.setCheckExistence(true);
    secondaryTemplateResolver.setCacheable(false);

    return secondaryTemplateResolver;
  }
}
