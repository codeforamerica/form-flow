package formflow.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Test-specific security configuration that disables secure cookies
 * to allow Selenium tests to access cookies over HTTP.
 */
@Profile("test")
@Configuration
public class TestSecurityConfiguration {

    /**
     * Overrides the default cookie serializer for tests to not use secure cookies.
     * This allows Selenium tests to access cookies over HTTP connections.
     *
     * @return serializer with secure cookies disabled for test environment
     */
    @Bean
    @Primary
    public DefaultCookieSerializer setDefaultSecurityCookie() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setUseSecureCookie(false); // Disable secure cookies for tests
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
