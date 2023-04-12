package formflow.library.config;

import formflow.library.pdf.PdfMapConfiguration;
import formflow.library.pdf.PdfMapConfiguration.PdfMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Parses the pdf map yaml file and creates corresponding PdfFieldMap Beans.
 */
public class PdfMapConfigurationFactory implements FactoryBean<List<PdfMap>> {

  //  @Value("${form-flow.path:flows-config.yaml}")
  String configPath = "pdf-map.yaml";

  @Override
  public List<PdfMap> getObject() {
    ClassPathResource classPathResource = new ClassPathResource(configPath);

    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
    loaderOptions.setAllowRecursiveKeys(true);

    Yaml yaml = new Yaml(new Constructor(PdfMap.class), new Representer(),
        new DumperOptions(), loaderOptions);
    List<PdfMap> pdfMaps = new ArrayList<>();
    try {
      Iterable<Object> pdfMapConfigurationIterable = yaml.loadAll(classPathResource.getInputStream());
      pdfMapConfigurationIterable.forEach(pdfMapConfiguration -> {
        pdfMaps.add((PdfMap) pdfMapConfiguration);
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    return pdfMaps;
  }

  @Override
  public Class<?> getObjectType() {
    return PdfMapConfiguration.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
