package formflow.library.config;

import formflow.library.pdf.PdfMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PdfMapFactory implements FactoryBean<List<PdfMap>> {

  //@Value("${form-flow.pdf-map:pdf-foo.yaml}")
  //String configPath;
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
    List<PdfMap> pdfMap = new ArrayList<>();
    try {
      Iterable<Object> pdfMapConfigurationIterable = yaml.loadAll(classPathResource.getInputStream());
      pdfMapConfigurationIterable.forEach(map -> {
        pdfMap.add((PdfMap) map);
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    return pdfMap;
  }

  @Override
  public Class<?> getObjectType() {
    return PdfMap.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
