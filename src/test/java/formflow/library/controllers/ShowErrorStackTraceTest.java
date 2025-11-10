package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml", "form-flow.error.show-stack-trace=true",
        "form-flow.error.pretty-print-packages=formflow,org.springframework"}, webEnvironment = RANDOM_PORT)
public class ShowErrorStackTraceTest extends AbstractBasePageTest {

    @Test
    void showErrorPageWithStackTraceWhenPropertyEnabled() {
        navigateTo("flow/inputs/asdf");
        String errorMessages = testPage.findElementsByClass("data-table").get(0).getText();
        String stackTrace = testPage.findElementsByClass("stack-trace").get(0).getText();
        assertThat(errorMessages).containsSequence("Timestamp:");
        assertThat(errorMessages).containsSequence("Status: 404");
        assertThat(errorMessages).containsSequence(
                "Message: There was a problem with the request (flow: inputs, screen: null): Could not find flow inputs in your applications flow configuration file.");
        assertThat(errorMessages).containsSequence("Path: /flow/inputs/asdf");
        assertThat(errorMessages).containsSequence("Exception: org.springframework.web.server.ResponseStatusException");
        assertThat(stackTrace).containsSequence(
                "org.springframework.web.server.ResponseStatusException: 404 NOT_FOUND \"There was a problem with the request (flow: inputs, screen: null): Could not find flow inputs in your applications flow configuration file.");
    }

    @Test
    void showPackagesAreHighlightedBlueInStackTraceWhenListedInProperty() {
        navigateTo("flow/inputs/asdf");
        assertThat(testPage.getHtml()).containsSequence("<span style=\"color: #00bfff; font-weight:bold;\">\tat formflow");
        assertThat(testPage.getHtml()).containsSequence(
                "<span style=\"color: #00bfff; font-weight:bold;\">\tat org.springframework");
    }

}
