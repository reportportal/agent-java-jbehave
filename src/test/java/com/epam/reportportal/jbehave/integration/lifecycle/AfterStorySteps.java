/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeStory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfterStorySteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(AfterStorySteps.class);

	@BeforeStory
	public void afterStory() {
		LOGGER.info("Inside 'afterStory'");
	}
}
