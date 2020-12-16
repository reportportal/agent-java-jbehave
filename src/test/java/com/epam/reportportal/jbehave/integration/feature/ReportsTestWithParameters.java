package com.epam.reportportal.jbehave.integration.feature;

import com.epam.reportportal.annotations.ParameterKey;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportsTestWithParameters {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportsTestWithParameters.class);

	@Given("It is test with parameters")
	public void infoLevel() {
		LOGGER.info("It is test with parameters");
	}

	@When("I have parameter {string}")
	public void iHaveParameterStr(String str) {
		LOGGER.info("String parameter {}", str);
	}

	@When("I have a docstring parameter:")
	public void iHaveParameterDocstring(String str) {
		iHaveParameterStr(str);
	}

	@Then("I emit number {int} on level info")
	public void infoLevel(int parameters) {
		LOGGER.info("Test with parameters: " + parameters);
	}

	@Given("It is a step with an integer parameter $int_param")
	public void iHaveAnIntInlineParameter(@Named("int_param") int parameter) {
		LOGGER.info("Integer parameter: " + parameter);
	}

	@When("I have a step with a string parameter $str_param")
	public void iHaveAnStrInlineParameter(@Named("str_param") String str) {
		LOGGER.info("String parameter {}", str);
	}
}
