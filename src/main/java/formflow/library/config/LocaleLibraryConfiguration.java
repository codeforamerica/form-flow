package formflow.library.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * A locale configuration so that messages can be automatically read from the library and Spring Boot apps.
 */
@Configuration
public class LocaleLibraryConfiguration {

  /**
   * A bean to set message source locations both in this library and in the default path of Spring Boot apps.
   *
   * <p>setBasenames() will do three things:</p>
   * <ol>
   *   <li>It will override any previous setting of basenames. Only these names/paths exist now.</li>
   *   <li>t will look for messages in sequential order and if something is found in a file, it stops looking there.
   *        Order matters and if you want to provide overrides, make sure they are in the earlier
   *        files (like 'messages', rather than 'messages-form-flow').</li>
   *   <li>For i18n this will look for files with the basename + "_[lang]", like messages_en.properties,
   *        automatically for you.</li>
   * </ol>
   *
   * @return messageSource to be consumed by Spring Boot
   */
  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

    messageSource.setBasenames("messages", "messages-form-flow");

    return messageSource;
  }
}
