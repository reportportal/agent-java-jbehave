/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfterScenarioSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(AfterScenarioSteps.class);

	@BeforeScenario
	public void afterScenario() {
		LOGGER.info("Inside 'afterScenario'");
	}
}
