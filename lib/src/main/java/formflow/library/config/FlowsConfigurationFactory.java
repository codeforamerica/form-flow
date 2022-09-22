package formflow.library.config;

import formflow.library.FormFlowProperties;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlowsConfigurationFactory implements FactoryBean<List<FlowConfiguration>> {
  private final FormFlowProperties properties;

  @Value("${form-flow.path:flows-config.yaml}")
  String configPath;


  public FlowsConfigurationFactory(FormFlowProperties properties) {
    this.properties = properties;
  }

  public void getConfigPath() {
    System.out.println("drumroll please... " + properties.getPath());
  }

  @Override
  public List<FlowConfiguration> getObject() {
    System.out.println("what is this... " + properties.getPath());
    System.out.println("configPath is... " + configPath);
    ClassPathResource classPathResource = new ClassPathResource(properties.getPath());

    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
    loaderOptions.setAllowRecursiveKeys(true);

    Yaml yaml = new Yaml(new Constructor(FlowConfiguration.class), new Representer(),
        new DumperOptions(), loaderOptions);
    List<FlowConfiguration> appConfigs = new ArrayList<>();
    try {
      Iterable<Object> appConfigsIterable = yaml.loadAll(classPathResource.getInputStream());
      appConfigsIterable.forEach(appConfig -> {
        appConfigs.add((FlowConfiguration) appConfig);
      });
    } catch (IOException e) {
      e.printStackTrace();
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
