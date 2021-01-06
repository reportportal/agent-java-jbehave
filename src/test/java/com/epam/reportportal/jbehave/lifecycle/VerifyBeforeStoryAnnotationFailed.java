/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave.lifecycle;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.jbehave.integration.lifecycle.BeforeStoryFailedSteps;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class VerifyBeforeStoryAnnotationFailed extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String beforeStoryId = CommonUtils.namedId("before_story_");
	private final String beforeStepId = CommonUtils.namedId("before_story_step_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final List<Pair<String, List<String>>> steps = Arrays.asList(Pair.of(beforeStoryId, Collections.singletonList(beforeStepId)),
			Pair.of(scenarioId, Collections.singletonList(stepId))
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

	private static final String STORY_PATH = "stories/NoScenario.story";
	private static final String DEFAULT_SCENARIO_NAME = "No name";
	private static final String STEP_NAME = "Given I have empty step";

	@Test
	public void verify_before_story_annotation_failed_method_reporting() {
		run(format, STORY_PATH, new BeforeStoryFailedSteps(), new EmptySteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(storyId), startCaptor.capture());
		verify(client).startTestItem(same(beforeStoryId), startCaptor.capture());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		StartTestItemRQ beforeStoryStart = startItems.get(0);
		assertThat(beforeStoryStart.getName(), equalTo("BeforeStory"));
		assertThat(beforeStoryStart.getCodeRef(), nullValue());
		assertThat(beforeStoryStart.getType(), equalTo(ItemType.TEST.name()));

		String scenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", DEFAULT_SCENARIO_NAME);
		StartTestItemRQ scenarioStart = startItems.get(1);
		assertThat(scenarioStart.getName(), equalTo(DEFAULT_SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		StartTestItemRQ beforeStep = startItems.get(2);
		String beforeStepCodeRef = BeforeStoryFailedSteps.class.getCanonicalName() + ".beforeStoryFailed()";
		assertThat(beforeStep.getName(), equalTo(beforeStepCodeRef));
		assertThat(beforeStep.getCodeRef(), equalTo(beforeStepCodeRef));
		assertThat(beforeStep.getType(), equalTo(ItemType.BEFORE_SUITE.name()));

		StartTestItemRQ step = startItems.get(3);
		String stepCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", STEP_NAME);
		assertThat(step.getName(), equalTo(STEP_NAME));
		assertThat(step.getCodeRef(), equalTo(stepCodeRef));
		assertThat(step.getType(), equalTo(ItemType.STEP.name()));

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(beforeStepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(stepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(beforeStoryId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		FinishTestItemRQ beforeStepFinish = finishItems.get(0);
		assertThat(beforeStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(beforeStepFinish.getIssue(), nullValue());

		FinishTestItemRQ stepFinish = finishItems.get(1);
		assertThat(stepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ beforeScenarioFinish = finishItems.get(2);
		assertThat(beforeScenarioFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));

		FinishTestItemRQ scenarioFinish = finishItems.get(3);
		assertThat(scenarioFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ storyFinish = finishItems.get(4);
		assertThat(storyFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
	}
}
