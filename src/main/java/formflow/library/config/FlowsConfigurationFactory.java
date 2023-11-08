package formflow.library.config;

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
 * Parses the flow configuration yaml file and setups the FlowConfiguration list.
 */
@Slf4j
public class FlowsConfigurationFactory implements FactoryBean<List<FlowConfiguration>> {

  @Value("${form-flow.path:flows-config.yaml}")
  String configPath;
  
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
    ClassPathResource classPathResource = new ClassPathResource(configPath);

    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
    loaderOptions.setAllowRecursiveKeys(true);

    Yaml yaml = new Yaml(new Constructor(FlowConfiguration.class, loaderOptions), new Representer(new DumperOptions()),
        new DumperOptions(), loaderOptions);
    List<FlowConfiguration> appConfigs = new ArrayList<>();
    try {
      Iterable<Object> appConfigsIterable = yaml.loadAll(classPathResource.getInputStream());
      appConfigsIterable.forEach(appConfig -> {
        FlowConfiguration flowConfig = (FlowConfiguration) appConfig;
        if (formFlowConfigurationProperties == null || !formFlowConfigurationProperties.isFlowDisabled(flowConfig.getName())) {
          appConfigs.add(flowConfig);
        }
        
      });
    } catch (IOException e) {
      log.error("Can't find the flow configuration file: " + configPath, e);
      throw e;
    }

    return appConfigs;
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
