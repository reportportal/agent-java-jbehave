/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.integration.lifecycle;

import org.jbehave.core.annotations.BeforeStory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeStorySteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeforeStorySteps.class);

	@BeforeStory
	public void beforeStory() {
		LOGGER.info("Inside 'beforeStory'");
	}
}
