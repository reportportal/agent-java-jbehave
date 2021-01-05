/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.basic;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.jbehave.core.model.ExamplesTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(StockSteps.class);

	@Given("a stock of symbol <symbol> and a threshold <threshold>")
	public void aStock(@Named("symbol") String symbol, @Named("threshold") double threshold) {
		LOGGER.info("Got symbol '{}' and threshold '{}'", symbol, threshold);
	}

	@When("the stock is traded at price <price>")
	public void traded(@Named("price") Double price) {
		LOGGER.info("Got price '{}'", price);
	}

	@Then("the alert status should be status <status>")
	public void status(@Named("status") String status) {
		LOGGER.info("Got status '{}'", status);
	}

	@Given("the traders: $ranksTable")
	public void theTraders(ExamplesTable ranksTable) {
		StringBuilder sb = new StringBuilder();
		sb.append("Parameters table:\n");
		ranksTable.getRows().forEach(params -> {
			params.forEach((k,v) -> sb.append(String.format("Got parameter '%s':'%s'\n", k, v)));
			sb.append("------------------\n");
		});
		LOGGER.info(sb.toString());
	}
}
