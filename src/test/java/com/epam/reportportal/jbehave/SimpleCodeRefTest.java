/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

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

public class SimpleCodeRefTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(2).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client, standardParameters()));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepIds);
		mockBatchLogging(client);
	}

	private static final String SIMPLE_CODE_REFERENCE_STORY = "stories/DummyScenario.story";
	private static final List<String> DUMMY_SCENARIO_STEPS = Arrays.asList("Given I have empty step", "Then I have another empty step");

	@Test
	public void verify_code_reference_generation() {
		run(format, SIMPLE_CODE_REFERENCE_STORY);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(storyId), captor.capture());
		verify(client, times(2)).startTestItem(same(scenarioId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(4));

		StartTestItemRQ storyRq = items.get(0);
		StartTestItemRQ scenarioRq = items.get(1);

		String storyCodeRef = SIMPLE_CODE_REFERENCE_STORY;
		assertThat(storyRq.getCodeRef(), allOf(notNullValue(), equalTo(storyCodeRef)));
		assertThat(storyRq.getType(), allOf(notNullValue(), equalTo(ItemType.STORY.name())));

		String scenarioCodeRef = storyCodeRef + "/[SCENARIO:The scenario]";
		assertThat(scenarioRq.getCodeRef(), allOf(notNullValue(), equalTo(scenarioCodeRef)));
		assertThat(scenarioRq.getType(), allOf(notNullValue(), equalTo(ItemType.SCENARIO.name())));

		List<StartTestItemRQ> stepRqs = items.subList(2, items.size());
		IntStream.range(0, stepRqs.size()).forEach(i -> {
			StartTestItemRQ step = stepRqs.get(i);
			String stepCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", DUMMY_SCENARIO_STEPS.get(i));
			assertThat(step.getCodeRef(), allOf(notNullValue(), equalTo(stepCodeRef)));
			assertThat(step.getType(), allOf(notNullValue(), equalTo(ItemType.STEP.name())));
		});
	}
}
