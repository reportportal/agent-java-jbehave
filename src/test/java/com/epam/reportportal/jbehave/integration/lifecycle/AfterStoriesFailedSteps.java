/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.AfterStories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfterStoriesFailedSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(AfterStoriesFailedSteps.class);

	public static final String ERROR_MESSAGE = "A failed after all stories step";

	@AfterStories
	public void afterStoriesFailed() {
		LOGGER.info("Inside 'afterStoriesFailed'");
		throw new IllegalStateException(ERROR_MESSAGE);
	}
}
