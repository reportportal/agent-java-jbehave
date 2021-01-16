/*
 * Copyright 2021 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.jbehave.lifecycle;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.jbehave.integration.lifecycle.AfterStoryFailedSteps;
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

public class VerifyAfterStoryAnnotationFailed extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String afterStoryId = CommonUtils.namedId("after_scenario_");
	private final String afterStepId = CommonUtils.namedId("after_step_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final List<Pair<String, List<String>>> steps = Arrays.asList(Pair.of(scenarioId, Collections.singletonList(stepId)),
			Pair.of(afterStoryId, Collections.singletonList(afterStepId))
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
	public void verify_after_story_annotation_failed_method_reporting() {
		run(format, STORY_PATH, new AfterStoryFailedSteps(), new EmptySteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(storyId), startCaptor.capture());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());
		verify(client).startTestItem(same(afterStoryId), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		String scenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", DEFAULT_SCENARIO_NAME);
		StartTestItemRQ scenarioStart = startItems.get(0);
		assertThat(scenarioStart.getName(), equalTo(DEFAULT_SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		StartTestItemRQ beforeStoryStart = startItems.get(1);
		assertThat(beforeStoryStart.getName(), equalTo("AfterStory"));
		assertThat(beforeStoryStart.getCodeRef(), nullValue());
		assertThat(beforeStoryStart.getType(), equalTo(ItemType.TEST.name()));

		StartTestItemRQ step = startItems.get(2);
		String stepCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", STEP_NAME);
		assertThat(step.getName(), equalTo(STEP_NAME));
		assertThat(step.getCodeRef(), equalTo(stepCodeRef));
		assertThat(step.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ afterStep = startItems.get(3);
		String beforeStepCodeRef = AfterStoryFailedSteps.class.getCanonicalName() + ".afterStoryFailed()";
		assertThat(afterStep.getName(), equalTo(beforeStepCodeRef));
		assertThat(afterStep.getCodeRef(), equalTo(beforeStepCodeRef));
		assertThat(afterStep.getType(), equalTo(ItemType.AFTER_SUITE.name()));

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(stepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(afterStepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(afterStoryId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		FinishTestItemRQ stepFinish = finishItems.get(0);
		assertThat(stepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ scenarioFinish = finishItems.get(1);
		assertThat(scenarioFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ afterStepFinish = finishItems.get(2);
		assertThat(afterStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(afterStepFinish.getIssue(), nullValue());

		FinishTestItemRQ afterStoryFinish = finishItems.get(3);
		assertThat(afterStoryFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));

		FinishTestItemRQ storyFinish = finishItems.get(4);
		assertThat(storyFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
	}
}
