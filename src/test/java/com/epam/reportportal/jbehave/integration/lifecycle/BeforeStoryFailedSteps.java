/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeStory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeStoryFailedSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeStoryFailedSteps.class);

	public static final String ERROR_MESSAGE = "A failed before story step";

	@BeforeStory
	public void beforeStoryFailed() {
		LOGGER.info("Inside 'beforeStoryFailed'");
		throw new IllegalStateException(ERROR_MESSAGE);
	}
}
