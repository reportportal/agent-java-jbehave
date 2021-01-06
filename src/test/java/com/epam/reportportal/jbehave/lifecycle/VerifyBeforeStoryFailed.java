/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.lifecycle;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.jbehave.integration.basic.FailedSteps;
import com.epam.reportportal.jbehave.integration.basic.ParameterizedSteps;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
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
import java.util.stream.Stream;

import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class VerifyBeforeStoryFailed extends BaseTest {

	public static final String STEP_PATTERN = "/[STEP:%s]";
	private final String storyId = namedId("story_");
	private final List<String> lifecycleStepIds = Arrays.asList(namedId("before_story_"),
			namedId("before_scenario_"),
			namedId("before_step_"),
			namedId("after_step_"),
			namedId("after_scenario_"),
			namedId("after_story_")
	);
	private final String scenarioId = namedId("scenario_");
	private final String stepId = namedId("step_");

	private final List<Pair<String, List<String>>> steps = Arrays.asList(Pair.of(lifecycleStepIds.get(0), Collections.emptyList()),
			Pair.of(scenarioId, Stream.concat(Stream.concat(lifecycleStepIds.subList(1, 3).stream(), Stream.of(stepId)),
					lifecycleStepIds.subList(3, lifecycleStepIds.size() - 1).stream()
			).collect(Collectors.toList())),
			Pair.of(lifecycleStepIds.get(lifecycleStepIds.size() - 1), Collections.emptyList())
	);

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

	private static final String STORY_PATH = "stories/lifecycle/BeforeStoryFailed.story";
	private static final String SCENARIO_NAME = "The scenario";
	private static final String STEP_NAME = "Given I have empty step";
	private static final String[] LIFECYCLE_STEP_NAMES = new String[] { "Given I have a failed step", "Then I have another empty step",
			"Given It is test with parameters", "When I have parameter test", "Then I have another empty step",
			"Given It is a step with an integer parameter 42" };

	@Test
	public void verify_before_story_lifecycle_step_failure_reporting() {
		run(format, STORY_PATH, new EmptySteps(), new ParameterizedSteps(), new FailedSteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(3)).startTestItem(same(storyId), startCaptor.capture());
		verify(client, times(5)).startTestItem(same(scenarioId), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();

		StartTestItemRQ beforeStoryStart = startItems.get(0);
		assertThat(beforeStoryStart.getName(), equalTo(LIFECYCLE_STEP_NAMES[0]));
		assertThat(beforeStoryStart.getCodeRef(), equalTo(STORY_PATH + String.format(STEP_PATTERN, LIFECYCLE_STEP_NAMES[0])));
		assertThat(beforeStoryStart.getType(), equalTo(ItemType.STEP.name()));

		String scenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", SCENARIO_NAME);
		StartTestItemRQ scenarioStart = startItems.get(1);
		assertThat(scenarioStart.getName(), equalTo(SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		StartTestItemRQ beforeScenario = startItems.get(3);
		String beforeScenarioCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, LIFECYCLE_STEP_NAMES[1]);
		assertThat(beforeScenario.getName(), equalTo(LIFECYCLE_STEP_NAMES[1]));
		assertThat(beforeScenario.getCodeRef(), equalTo(beforeScenarioCodeRef));
		assertThat(beforeScenario.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ beforeStep = startItems.get(4);
		String beforeStepCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, LIFECYCLE_STEP_NAMES[2]);
		assertThat(beforeStep.getName(), equalTo(LIFECYCLE_STEP_NAMES[2]));
		assertThat(beforeStep.getCodeRef(), equalTo(beforeStepCodeRef));
		assertThat(beforeStep.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ step = startItems.get(5);
		String stepCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, STEP_NAME);
		assertThat(step.getName(), equalTo(STEP_NAME));
		assertThat(step.getCodeRef(), equalTo(stepCodeRef));
		assertThat(step.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ afterStep = startItems.get(6);
		String afterStepCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, LIFECYCLE_STEP_NAMES[3]);
		assertThat(afterStep.getName(), equalTo(LIFECYCLE_STEP_NAMES[3]));
		assertThat(afterStep.getCodeRef(), equalTo(afterStepCodeRef));
		assertThat(afterStep.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ afterScenario = startItems.get(7);
		String afterScenarioCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, LIFECYCLE_STEP_NAMES[4]);
		assertThat(afterScenario.getName(), equalTo(LIFECYCLE_STEP_NAMES[4]));
		assertThat(afterScenario.getCodeRef(), equalTo(afterScenarioCodeRef));
		assertThat(afterScenario.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ afterStory = startItems.get(2);
		String afterStoryCodeRef = STORY_PATH + String.format(STEP_PATTERN, LIFECYCLE_STEP_NAMES[LIFECYCLE_STEP_NAMES.length - 1]);
		assertThat(afterStory.getName(), equalTo(LIFECYCLE_STEP_NAMES[LIFECYCLE_STEP_NAMES.length - 1]));
		assertThat(afterStory.getCodeRef(), equalTo(afterStoryCodeRef));
		assertThat(afterStory.getType(), equalTo(ItemType.STEP.name()));

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		lifecycleStepIds.forEach(s -> verify(client).finishTestItem(same(s), finishStepCaptor.capture()));
		verify(client).finishTestItem(same(stepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		List<FinishTestItemRQ> failedItems = Arrays.asList(finishItems.get(0), finishItems.get(finishItems.size() - 1));
		List<FinishTestItemRQ> passedItems = finishItems.subList(1, finishItems.size() - 1);
		failedItems.forEach(i -> assertThat(i.getStatus(), equalTo(ItemStatus.FAILED.name())));
		passedItems.forEach(i -> assertThat(i.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
