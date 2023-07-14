package formflow.library.address_validation;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class AddressValidationServiceTestConfiguration {

  @Bean
  @Primary
  public AddressValidationService addressValidationService() {
    return Mockito.mock(AddressValidationService.class);
  }
}
