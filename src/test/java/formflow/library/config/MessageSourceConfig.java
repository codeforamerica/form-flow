package formflow.library.config;

import static java.util.Locale.ENGLISH;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource rs = new ResourceBundleMessageSource();
    rs.setBasenames("messages-form-flow");
    rs.setDefaultLocale(ENGLISH);
    rs.setUseCodeAsDefaultMessage(true);
    return rs;
  }

}
