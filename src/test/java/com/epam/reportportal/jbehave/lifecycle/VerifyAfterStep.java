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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class VerifyAfterStep extends BaseTest {

	private static final int STEP_NUMBER = 2;
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> afterStepIds = Stream.generate(() -> CommonUtils.namedId("after_step_"))
			.limit(STEP_NUMBER)
			.collect(Collectors.toList());
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_"))
			.limit(STEP_NUMBER)
			.collect(Collectors.toList());

	private final List<Pair<String, List<String>>> steps = Collections.singletonList(Pair.of(scenarioId,
			IntStream.range(0, stepIds.size())
					.mapToObj(i -> Stream.of(stepIds.get(i), afterStepIds.get(i)))
					.flatMap(i -> i)
					.collect(Collectors.toList())
	));

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

	private static final String STORY_PATH = "stories/lifecycle/AfterStep.story";
	private static final String SCENARIO_NAME = "The scenario";
	private static final String[] STEP_NAMES = new String[] { "Given I have empty step", "When I have one more empty step" };
	private static final String LIFECYCLE_STEP_NAME = "Then I have another empty step";

	@Test
	public void verify_after_step_lifecycle_step_reporting() {
		run(format, STORY_PATH, new EmptySteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(storyId), startCaptor.capture());
		verify(client, times(STEP_NUMBER * 2)).startTestItem(same(scenarioId), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		StartTestItemRQ scenarioStart = startItems.get(0);
		String scenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", SCENARIO_NAME);
		assertThat(scenarioStart.getName(), equalTo(SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		List<StartTestItemRQ> steps = Arrays.asList(startItems.get(1), startItems.get(3));
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ step = steps.get(i);
			String stepCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", STEP_NAMES[i]);
			assertThat(step.getName(), equalTo(STEP_NAMES[i]));
			assertThat(step.getCodeRef(), equalTo(stepCodeRef));
			assertThat(step.getType(), equalTo(ItemType.STEP.name()));
		});

		List<StartTestItemRQ> afterStarts = Arrays.asList(startItems.get(2), startItems.get(4));
		IntStream.range(0, afterStarts.size()).forEach(i -> {
			StartTestItemRQ beforeStart = afterStarts.get(i);
			String lifecycleCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", LIFECYCLE_STEP_NAME);
			assertThat(beforeStart.getName(), equalTo(LIFECYCLE_STEP_NAME));
			assertThat(beforeStart.getCodeRef(), equalTo(lifecycleCodeRef));
			assertThat(beforeStart.getType(), equalTo(ItemType.STEP.name()));
		});

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(stepIds.get(0)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(afterStepIds.get(0)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(stepIds.get(1)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(afterStepIds.get(1)), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		finishItems.forEach(i -> assertThat(i.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
