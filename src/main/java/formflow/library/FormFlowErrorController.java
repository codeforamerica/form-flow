package formflow.library;

import static org.springframework.boot.web.error.ErrorAttributeOptions.Include.BINDING_ERRORS;
import static org.springframework.boot.web.error.ErrorAttributeOptions.Include.EXCEPTION;
import static org.springframework.boot.web.error.ErrorAttributeOptions.Include.MESSAGE;
import static org.springframework.boot.web.error.ErrorAttributeOptions.Include.STACK_TRACE;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FormFlowErrorController implements ErrorController {

  private final boolean showStackTrace;
  private final String errorPath;

  private final String prettyPrintPackages;
  private final ErrorAttributes errorAttributes;

  public FormFlowErrorController(
      @Value("${form-flow.error.show-stack-trace:false}") boolean showStackTrace,
      @Value("${server.error.path:errors/genericError}") String errorPath,
      @Value("${form-flow.error.pretty-print-packages:formflow}") String prettyPrintPackages,
      ErrorAttributes errorAttributes) {
    this.showStackTrace = showStackTrace;
    this.errorPath = errorPath;
    this.errorAttributes = errorAttributes;
    this.prettyPrintPackages = prettyPrintPackages;
  }

  @RequestMapping("/error")
  public ModelAndView handleError(WebRequest webRequest) {
    String prettyException = prettyPrintStackTrace(errorAttributes.getError(webRequest));

    Map<String, Object> errorAttributesMap = errorAttributes.getErrorAttributes(
        webRequest,
        ErrorAttributeOptions.defaults().including(BINDING_ERRORS, EXCEPTION, MESSAGE, STACK_TRACE)
    );
    errorAttributesMap.put("prettyStackTrace", prettyException);
    if (showStackTrace) {
      return new ModelAndView("errors/devError", errorAttributesMap);
    } else {
      return new ModelAndView(errorPath, errorAttributesMap);
    }
  }

  private String prettyPrintStackTrace(Throwable e) {
    if (e == null) {
      return "";
    }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    String stackTrace = sw.toString();
    String[] lines = stackTrace.split("\\r?\\n");

    StringBuilder prettyStackTrace = new StringBuilder();
    prettyStackTrace.append("<pre>");

    for (String line : lines) {
      if (Arrays.stream(prettyPrintPackages.split(",")).anyMatch(pkg -> line.contains("at %s.".formatted(pkg)))) {
        prettyStackTrace.append("<span style=\"color: #00bfff; font-weight:bold;\">").append(line).append("</span><br>");
      } else if (line.contains("Caused by:")) {
        prettyStackTrace.append("<span style=\"color: red; font-weight:bold;\">").append(line).append("</span><br>");
      } else if (line.contains("Exception:") || line.contains("Error:")) {
        prettyStackTrace.append("<span style=\"color: red; font-weight:bold;\">").append(line).append("</span><br>");
      } else {
        prettyStackTrace.append(line).append("<br>");
      }
    }

    prettyStackTrace.append("</pre>");

    return prettyStackTrace.toString();
  }

}
