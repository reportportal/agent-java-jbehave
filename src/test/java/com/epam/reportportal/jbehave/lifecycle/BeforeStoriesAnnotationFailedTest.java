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
import com.epam.reportportal.jbehave.integration.lifecycle.BeforeStoriesFailedSteps;
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
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class BeforeStoriesAnnotationFailedTest extends BaseTest {

	private final String beforeStoriesId = CommonUtils.namedId("before_stories_");
	private final String beforeStepId = CommonUtils.namedId("before_story_step_");
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final List<Pair<String, List<String>>> beforeSteps = Collections.singletonList(Pair.of(beforeStoriesId,
			Collections.singletonList(beforeStepId)
	));

	private final List<Pair<String, List<String>>> steps = Collections.singletonList(Pair.of(scenarioId,
			Collections.singletonList(stepId)
	));

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, beforeStoriesId, beforeSteps);
		mockStories(client, Arrays.asList(Pair.of(beforeStoriesId, beforeSteps), Pair.of(storyId, steps)));
		mockBatchLogging(client);
	}

	private static final String STORY_PATH = "stories/NoScenario.story";
	private static final String DEFAULT_SCENARIO_NAME = "No name";
	private static final String STEP_NAME = "Given I have empty step";

	private static final String AFTER_STORY_NAME = "beforeStoriesFailed";

	@Test
	public void verify_before_story_annotation_failed_method_reporting() {
		run(format, STORY_PATH, new BeforeStoriesFailedSteps(), new EmptySteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(startCaptor.capture());
		verify(client).startTestItem(same(beforeStoriesId), startCaptor.capture());
		verify(client).startTestItem(same(storyId), startCaptor.capture());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		StartTestItemRQ beforeStoriesStart = startItems.get(0);
		assertThat(beforeStoriesStart.getName(), equalTo("BeforeStories"));
		assertThat(beforeStoriesStart.getType(), equalTo(ItemType.TEST.name()));

		StartTestItemRQ storyStart = startItems.get(1);
		assertThat(storyStart.getCodeRef(), allOf(notNullValue(), equalTo(STORY_PATH)));
		assertThat(storyStart.getType(), allOf(notNullValue(), equalTo(ItemType.STORY.name())));

		StartTestItemRQ beforeStep = startItems.get(2);
		assertThat(beforeStep.getName(), equalTo(AFTER_STORY_NAME));
		assertThat(beforeStep.getCodeRef(), equalTo(AFTER_STORY_NAME));
		assertThat(beforeStep.getType(), equalTo(ItemType.BEFORE_GROUPS.name()));

		String scenarioCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", DEFAULT_SCENARIO_NAME);
		StartTestItemRQ scenarioStart = startItems.get(3);
		assertThat(scenarioStart.getName(), equalTo(DEFAULT_SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		StartTestItemRQ step = startItems.get(4);
		String stepCodeRef = scenarioCodeRef + String.format("/[STEP:%s]", STEP_NAME);
		assertThat(step.getName(), equalTo(STEP_NAME));
		assertThat(step.getCodeRef(), equalTo(stepCodeRef));
		assertThat(step.getType(), equalTo(ItemType.STEP.name()));

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(beforeStepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(beforeStoriesId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(stepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		FinishTestItemRQ beforeStepFinish = finishItems.get(0);
		assertThat(beforeStepFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(beforeStepFinish.getIssue(), nullValue());

		FinishTestItemRQ beforeStoryFinish = finishItems.get(1);
		assertThat(beforeStoryFinish.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(beforeStoryFinish.getIssue(), nullValue());

		FinishTestItemRQ stepFinish = finishItems.get(2);
		assertThat(stepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ scenarioFinish = finishItems.get(3);
		assertThat(scenarioFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ storyFinish = finishItems.get(4);
		assertThat(storyFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));
	}
}
