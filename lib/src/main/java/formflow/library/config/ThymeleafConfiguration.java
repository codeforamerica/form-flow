package formflow.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class ThymeleafConfiguration {
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
