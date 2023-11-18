package formflow.library.config;

import formflow.library.exceptions.FlowConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Parses the flow configuration yaml file and adds validated flow configuration objects to the libraries list of flow configurations.
 */
@Slf4j
public class FlowsConfigurationFactory implements FactoryBean<List<FlowConfiguration>> {

  @Value("${form-flow.path:flows-config.yaml}")
  String configPath;
  
  @Value("${form-flow.session-continuity-interceptor.enabled:false}")
  boolean sessionContinuityInterceptorEnabled;
  
  FormFlowConfigurationProperties formFlowConfigurationProperties;

  FlowsConfigurationFactory() {
    this.formFlowConfigurationProperties = null;
  }

  FlowsConfigurationFactory(FormFlowConfigurationProperties formFlowConfigurationProperties) {
    this.formFlowConfigurationProperties = formFlowConfigurationProperties;
  }

  /**
   * Takes in the flow configuration yaml file from the given form-flow.path application properties and parses it into a list of 
   * FlowConfiguration objects.
   * 
   * Disabled flows will be excluded, detected using the form-flow.disabled-flows application property.
   * @return list of FlowConfiguration objects.
   * @throws IOException if the flow configuration file can't be found.
   */
  @Override
  public List<FlowConfiguration> getObject() throws IOException {
    List<FlowConfiguration> flowConfigurations = new ArrayList<>();
    try {
      Iterable<Object> flowConfigsIterable = loadFlowConfigurationsFromYaml();
      flowConfigsIterable.forEach(flowConfig -> addValidatedFlowConfiguration((FlowConfiguration) flowConfig, flowConfigurations));
    } catch (IOException e) {
      log.error("Can't find the flow configuration file: " + configPath, e);
      throw e;
    }

    return flowConfigurations;
  }
  
  private Iterable<Object> loadFlowConfigurationsFromYaml() throws IOException {
    ClassPathResource classPathResource = new ClassPathResource(configPath);

    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
    loaderOptions.setAllowRecursiveKeys(true);
    
    Yaml yaml = new Yaml(new Constructor(FlowConfiguration.class, loaderOptions), new Representer(new DumperOptions()),
        new DumperOptions(), loaderOptions);
    return yaml.loadAll(classPathResource.getInputStream());
  }

  private void addValidatedFlowConfiguration(FlowConfiguration flowConfig, List<FlowConfiguration> flowConfigurations) {
    if (shouldAddFlowConfiguration(flowConfig)) {
      validateFlowConfiguration(flowConfig);
      flowConfigurations.add(flowConfig);
    }
  }

  private boolean shouldAddFlowConfiguration(FlowConfiguration flowConfig) {
    return formFlowConfigurationProperties == null || !formFlowConfigurationProperties.isFlowDisabled(flowConfig.getName());
  }

  private void validateFlowConfiguration(FlowConfiguration flowConfig) {
    if (formFlowConfigurationProperties != null && formFlowConfigurationProperties.isSubmissionLockedForFlow(flowConfig.getName())) {
        validateLandmarksAfterSubmitPages(flowConfig);
    }
    if (sessionContinuityInterceptorEnabled) {
      validateLandmarksFirstScreen(flowConfig);
    }
  }

  private void validateLandmarksAfterSubmitPages(FlowConfiguration flowConfig) {
    if (flowConfig.getLandmarks() == null || flowConfig.getLandmarks().getAfterSubmitPages() == null) {
      throw new FlowConfigurationException("You have enabled submission locking for the flow " + flowConfig.getName() + 
          " but the afterSubmitPages landmark is not set in your flow configuration yaml file.");
    }
  }

  protected void validateLandmarksFirstScreen(FlowConfiguration flowConfig) {
    if (flowConfig.getLandmarks() == null || flowConfig.getLandmarks().getFirstScreen() == null) {
      throw new FlowConfigurationException("You have enabled the session continuity interceptor in your application but have not added a first screen landmark for the flow " + flowConfig.getName() +
          " in your flow configuration yaml file.");
    }

    String firstScreen = flowConfig.getLandmarks().getFirstScreen();

    if (!flowConfig.getFlow().containsKey(firstScreen)) {
      throw new FlowConfigurationException(String.format(
          "Your flow configuration file for the flow %s does not contain a screen with the name '%s'. " +
              "You may have misspelled the screen name. Please make sure to correctly set the 'firstScreen' in the flows configuration file 'landmarks' section.",
          flowConfig.getName(), firstScreen));
    }
  }

  @Override
  public Class<?> getObjectType() {
    return FlowConfiguration.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
