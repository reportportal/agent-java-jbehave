/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.utils.TestUtils;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class DescriptionTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(2).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private ReportPortalStepFormat format;

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, storyId, scenarioId, stepIds);
		TestUtils.mockBatchLogging(client);
		format = new ReportPortalStepFormat(ReportPortal.create(client, TestUtils.standardParameters()));
	}

	private static final String SIMPLE_STORY = "stories/DummyScenario.story";

	@Test
	public void verify_story_description() {
		TestUtils.run(format, SIMPLE_STORY);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(storyId), any());
		verify(client, times(2)).startTestItem(same(scenarioId), any());

		StartTestItemRQ storyRq = captor.getValue();
		assertThat(storyRq.getDescription(), equalTo("A story description"));
	}
}
