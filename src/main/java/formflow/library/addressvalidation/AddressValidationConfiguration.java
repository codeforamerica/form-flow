package formflow.library.addressvalidation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for address validation. Provides the default SmartyStreets implementation
 * when no custom AddressValidationService bean is provided by the consuming application.
 */
@Configuration
public class AddressValidationConfiguration {

    /**
     * Provides the default SmartyStreetsAddressValidationService bean when no custom
     * AddressValidationService implementation is provided.
     *
     * @param validationRequestFactory Factory for creating validation requests.
     * @param clientFactory            Factory for creating SmartyStreets clients.
     * @param authId                   The authentication ID for the SmartyStreets API.
     * @param authToken                The authentication token for the SmartyStreets API.
     * @param license                  The license key for the SmartyStreets API.
     * @param isEnabled                Application property indicating if address validation is enabled.
     * @return An instance of SmartyStreetsAddressValidationService if no custom implementation exists.
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
        return new SmartyStreetsAddressValidationService(validationRequestFactory, clientFactory, authId, authToken, license, isEnabled);
    }
}

