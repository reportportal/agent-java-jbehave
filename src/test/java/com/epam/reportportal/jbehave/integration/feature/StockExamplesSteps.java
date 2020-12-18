/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.feature;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class StockExamplesSteps {

	@Given("a stock of <symbol> and a <threshold>")
	public void aStock(@Named("symbol") String symbol, @Named("threshold") double threshold) {

	}

	@When("the stock is traded at <price>")
	public void traded(@Named("price") Double price) {

	}

	@Then("the alert status should be <status>")
	public void status(@Named("status") String status) {

	}
}
