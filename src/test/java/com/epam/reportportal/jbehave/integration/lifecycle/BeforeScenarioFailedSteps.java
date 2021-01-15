/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeScenarioFailedSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeScenarioFailedSteps.class);

	public static final String ERROR_MESSAGE = "A failed before scenario step";

	@BeforeScenario
	public void beforeScenarioFailed() {
		LOGGER.info("Inside 'beforeScenarioFailed'");
		throw new IllegalStateException(ERROR_MESSAGE);
	}
}
