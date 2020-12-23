/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeScenarioSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeScenarioSteps.class);

	@BeforeScenario
	public void beforeScenario() {
		LOGGER.info("Inside 'beforeScenario'");
	}
}
