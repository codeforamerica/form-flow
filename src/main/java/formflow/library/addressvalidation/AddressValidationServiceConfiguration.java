package formflow.library.addressvalidation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for address validation service. Provides the default SmartyStreets implementation when no other
 * AddressValidationService bean is present.
 */
@Configuration
public class AddressValidationServiceConfiguration {

    /**
     * Creates the default SmartyStreets address validation service bean. This bean is only created if no other
     * AddressValidationService bean exists.
     *
     * @param validationRequestFactory Factory for creating validation requests.
     * @param clientFactory            Factory for creating SmartyStreets clients.
     * @param authId                   The authentication ID for the SmartyStreets API.
     * @param authToken                The authentication token for the SmartyStreets API.
     * @param license                  The license key for the SmartyStreets API.
     * @param isEnabled                Application property indicating if address validation is enabled.
     * @return The default SmartyStreets address validation service.
     */
    @Bean
    @ConditionalOnMissingBean(AddressValidationService.class)
    public AddressValidationService addressValidationService(
            ValidationRequestFactory validationRequestFactory,
            ClientFactory clientFactory,
            @Value("${form-flow.address-validation.smarty.auth-id:}") String authId,
            @Value("${form-flow.address-validation.smarty.auth-token:}") String authToken,
            @Value("${form-flow.address-validation.smarty.license:}") String license,
            @Value("${form-flow.address-validation.enabled:true}") boolean isEnabled) {
        return new SmartyStreetsAddressValidationService(
                validationRequestFactory,
                clientFactory,
                authId,
                authToken,
                license,
                isEnabled);
    }
}

