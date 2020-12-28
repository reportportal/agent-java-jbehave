/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeStories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeStoriesFailedSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeStoriesFailedSteps.class);

	public static final String ERROR_MESSAGE = "A failed before all stories step";

	@BeforeStories
	public void beforeStoriesFailed() {
		LOGGER.info("Inside 'beforeStoriesFailed'");
		throw new IllegalStateException(ERROR_MESSAGE);
	}
}
