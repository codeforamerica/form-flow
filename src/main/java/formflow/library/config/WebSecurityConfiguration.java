package formflow.library.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.session.web.http.DefaultCookieSerializer;


/**
 * Security features managed by Form-flow library
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    /**
     * Sets session cookies to HttpOny and Secure
     * @return serializer with updated cookie settings
     */
    @Bean
    public DefaultCookieSerializer setDefaultSecurityCookie(){
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setUseSecureCookie(true);
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
