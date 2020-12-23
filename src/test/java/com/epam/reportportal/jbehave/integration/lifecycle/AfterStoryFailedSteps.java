/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeStory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfterStoryFailedSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(AfterStoryFailedSteps.class);

	public static final String ERROR_MESSAGE = "A failed after story step";

	@BeforeStory
	public void afterStoryFailed() {
		LOGGER.info("Inside 'afterStoryFailed'");
		throw new IllegalStateException(ERROR_MESSAGE);
	}
}
