/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfterScenarioFailedSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(AfterScenarioFailedSteps.class);

	public static final String ERROR_MESSAGE = "A failed after scenario step";

	@BeforeScenario
	public void afterScenarioFailed() {
		LOGGER.info("Inside 'afterScenarioFailed'");
		throw new IllegalStateException(ERROR_MESSAGE);
	}
}
