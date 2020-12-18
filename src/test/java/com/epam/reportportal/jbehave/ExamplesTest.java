/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class ExamplesTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(5).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private ReportPortalStepFormat format;

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, storyId, scenarioId, stepIds);
		TestUtils.mockBatchLogging(client);
		format = new ReportPortalStepFormat(ReportPortal.create(client, TestUtils.standardParameters()));
	}

	@Test
	public void verify_story_with_examples() {
		TestUtils.run(format, "stories/Examples.story");


	}
}
