package formflow.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@EnableAutoConfiguration
@Slf4j
@Profile("dev")
@RequestMapping("dev")
public class DevController {

  /**
   * Renders a page that will show the current icons available.
   *
   * @return the icons html page
   */
  @GetMapping("/icons")
  String getIcons() {
    return "fragments/icons";
  }
}
