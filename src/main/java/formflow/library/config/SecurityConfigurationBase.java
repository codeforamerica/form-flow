package formflow.library.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Security Configuration - leverages spring security features Includes: - secure and httpOnly headers on cookies - no basicAuth -
 * utilize X-Forwarded features
 */
@Configuration
@EnableWebSecurity
public class SecurityConfigurationBase {

  /**
   * Sets session cookies to HttpOny and Secure
   *
   * @return serializer with updated cookie settings
   */
  @Bean
  public DefaultCookieSerializer setDefaultSecurityCookie() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    serializer.setUseSecureCookie(true);
    serializer.setUseHttpOnlyCookie(true);
    return serializer;
  }

  /**
   * Disables basic auth while retaining other security features
   *
   * @param httpSecurity HttpSecurity object
   * @return HttpSecurity configuration with authentication disabled
   * @throws Exception Internal configuration error
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(sessionManagementConfigurer -> {
              sessionManagementConfigurer
                      .invalidSessionUrl("/?sessionInvalid=true")
                      .maximumSessions(1)
                      .expiredUrl("/?sessionExpired=true");
            });
    return httpSecurity.build();
  }

  /**
   * Use X-Forwarded-For / X-Forwarded-Proto headers when generating full link URLs.
   *  
   * @return ForwardedHeaderFilter object
   */
  @Bean
  public ForwardedHeaderFilter forwardedHeaderFilter() {
    return new ForwardedHeaderFilter();
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }
}
