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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class BeforeStoryAnnotationFailedTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String beforeStoryId = CommonUtils.namedId("before_story_");
	private final String beforeStepId = CommonUtils.namedId("before_story_step_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final List<Pair<String, List<String>>> steps = Arrays.asList(Pair.of(
					beforeStoryId,
					Collections.singletonList(beforeStepId)
			),
			Pair.of(scenarioId, Collections.singletonList(stepId))
	);

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, steps);
		mockBatchLogging(client);
	}

	private static final String STORY_PATH = "stories/NoScenario.story";
	private static final String DEFAULT_SCENARIO_NAME = "No name";
	private static final String STEP_NAME = "Given I have empty step";
	private static final String BEFORE_STORY_NAME = "beforeStoryFailed";

	@Test
	public void verify_before_story_annotation_failed_method_reporting() {
		run(format, STORY_PATH, new BeforeStoryFailedSteps(), new EmptySteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> storyLevelCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(storyId), storyLevelCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> beforeLevelCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		if (!IS_JBEHAVE_5) {
			verify(client).startTestItem(same(beforeStoryId), beforeLevelCaptor.capture());
		}
		ArgumentCaptor<StartTestItemRQ> scenarioLevelCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(scenarioId), scenarioLevelCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> storyStartItems = storyLevelCaptor.getAllValues();
		StartTestItemRQ beforeStoryStart = storyStartItems.get(0);
		if (IS_JBEHAVE_5) {
			assertThat(beforeStoryStart.getName(), equalTo(BEFORE_STORY_NAME));
			assertThat(
					beforeStoryStart.getCodeRef(),
					equalTo(STORY_PATH + String.format("/[STEP:%s]", BEFORE_STORY_NAME))
			);
			assertThat(beforeStoryStart.getType(), equalTo(ItemType.STEP.name()));
		} else {
			assertThat(beforeStoryStart.getName(), equalTo("BeforeStory"));
			assertThat(beforeStoryStart.getCodeRef(), nullValue());
			assertThat(beforeStoryStart.getType(), equalTo(ItemType.TEST.name()));
		}

		String scenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", DEFAULT_SCENARIO_NAME);
		StartTestItemRQ scenarioStart = storyStartItems.get(1);
		assertThat(scenarioStart.getName(), equalTo(DEFAULT_SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		if(!IS_JBEHAVE_5) {
			StartTestItemRQ beforeStep = beforeLevelCaptor.getValue();
			String beforeStepCodeRef = BeforeStoryFailedSteps.class.getCanonicalName() + ".beforeStoryFailed()";
			assertThat(beforeStep.getName(), equalTo(beforeStepCodeRef));
			assertThat(beforeStep.getCodeRef(), equalTo(beforeStepCodeRef));
			assertThat(beforeStep.getType(), equalTo(ItemType.BEFORE_SUITE.name()));
		}

		StartTestItemRQ step = scenarioLevelCaptor.getValue();
		String stepCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", STEP_NAME);
		assertThat(step.getName(), equalTo(STEP_NAME));
		assertThat(step.getCodeRef(), equalTo(stepCodeRef));
		assertThat(step.getType(), equalTo(ItemType.STEP.name()));

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		if(!IS_JBEHAVE_5) {
			verify(client).finishTestItem(same(beforeStepId), finishStepCaptor.capture());
		}
		verify(client).finishTestItem(same(stepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(beforeStoryId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		if(!IS_JBEHAVE_5) {
			FinishTestItemRQ beforeStepFinish = finishItems.get(0);
			assertThat(beforeStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
			assertThat(beforeStepFinish.getIssue(), nullValue());
			finishItems = finishItems.subList(1, finishItems.size());
		}

		FinishTestItemRQ stepFinish = finishItems.get(0);
		assertThat(stepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ beforeScenarioFinish = finishItems.get(1);
		assertThat(beforeScenarioFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));

		FinishTestItemRQ scenarioFinish = finishItems.get(2);
		assertThat(scenarioFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ storyFinish = finishItems.get(3);
		assertThat(storyFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
	}
}
