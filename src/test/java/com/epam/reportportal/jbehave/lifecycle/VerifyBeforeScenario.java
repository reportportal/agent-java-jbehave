/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.lifecycle;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class VerifyBeforeScenario extends BaseTest {

	private static final int SCENARIO_NUMBER = 2;
	private final String storyId = CommonUtils.namedId("story_");
	private final List<String> scenarioIds = Stream.generate(() -> CommonUtils.namedId("scenario_"))
			.limit(SCENARIO_NUMBER)
			.collect(Collectors.toList());
	private final List<String> beforeStepIds = Stream.generate(() -> CommonUtils.namedId("before_scenario_step_"))
			.limit(SCENARIO_NUMBER)
			.collect(Collectors.toList());
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_"))
			.limit(SCENARIO_NUMBER)
			.collect(Collectors.toList());

	private final List<Pair<String, List<String>>> steps = IntStream.range(0, scenarioIds.size())
			.mapToObj(i -> Pair.of(scenarioIds.get(i), Arrays.asList(beforeStepIds.get(i), stepIds.get(i))))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			Executors.newSingleThreadExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, steps);
		mockBatchLogging(client);
	}

	private static final String STORY_PATH = "stories/lifecycle/BeforeScenario.story";
	private static final String[] SCENARIO_NAMES = new String[] { "The scenario", "Another scenario" };
	private static final String[] STEP_NAMES = new String[] { "Given I have empty step", "When I have one more empty step" };
	private static final String LIFECYCLE_STEP_NAME = "Then I have another empty step";

	@Test
	public void verify_before_scenario_lifecycle_step_reporting() {
		run(format, STORY_PATH, new EmptySteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(SCENARIO_NUMBER)).startTestItem(same(storyId), startCaptor.capture());
		verify(client, times(2)).startTestItem(same(scenarioIds.get(0)), startCaptor.capture());
		verify(client, times(2)).startTestItem(same(scenarioIds.get(1)), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		List<StartTestItemRQ> scenarioStarts = startItems.subList(0, 2);
		IntStream.range(0, scenarioStarts.size()).forEach(i -> {
			StartTestItemRQ scenarioStart = startItems.get(i);
			String firstScenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", SCENARIO_NAMES[i]);
			assertThat(scenarioStart.getName(), equalTo(SCENARIO_NAMES[i]));
			assertThat(scenarioStart.getCodeRef(), equalTo(firstScenarioCodeRef));
			assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));
		});

		List<StartTestItemRQ> beforeStarts = Arrays.asList(startItems.get(2), startItems.get(4));
		IntStream.range(0, beforeStarts.size()).forEach(i -> {
			StartTestItemRQ beforeStart = beforeStarts.get(i);
			String lifecycleCodeRef =
					STORY_PATH + String.format("/[SCENARIO:%s]", SCENARIO_NAMES[i]) + String.format("/[STEP:%s]", LIFECYCLE_STEP_NAME);
			assertThat(beforeStart.getName(), equalTo(LIFECYCLE_STEP_NAME));
			assertThat(beforeStart.getCodeRef(), equalTo(lifecycleCodeRef));
			assertThat(beforeStart.getType(), equalTo(ItemType.STEP.name()));
		});

		List<StartTestItemRQ> steps = Arrays.asList(startItems.get(3), startItems.get(5));
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ step = steps.get(i);
			String stepCodeRef =
					STORY_PATH + String.format("/[SCENARIO:%s]", SCENARIO_NAMES[i]) + String.format("/[STEP:%s]", STEP_NAMES[i]);
			assertThat(step.getName(), equalTo(STEP_NAMES[i]));
			assertThat(step.getCodeRef(), equalTo(stepCodeRef));
			assertThat(step.getType(), equalTo(ItemType.STEP.name()));
		});

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(beforeStepIds.get(0)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(stepIds.get(0)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioIds.get(0)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(beforeStepIds.get(1)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(stepIds.get(1)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioIds.get(1)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		finishItems.forEach(i -> assertThat(i.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
