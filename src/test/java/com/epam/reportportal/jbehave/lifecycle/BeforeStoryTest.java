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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class BeforeStoryTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String beforeSuiteId = CommonUtils.namedId("before_story_suite_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String beforeStepId = CommonUtils.namedId("before_story_step_");
	private final String stepId = CommonUtils.namedId("step_");

	private final List<Pair<String, List<String>>> steps = Arrays.asList(Pair.of(beforeSuiteId,
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

	private static final String STORY_PATH = "stories/lifecycle/BeforeStory.story";
	private static final String SCENARIO_NAME = "The scenario";
	private static final String STEP_NAME = "Given I have empty step";
	private static final String LIFECYCLE_SUITE_NAME = "BeforeStory";
	private static final String LIFECYCLE_STEP_NAME = "Then I have another empty step";

	@Test
	public void verify_before_story_lifecycle_step_reporting() {
		run(format, STORY_PATH, new EmptySteps());

		verify(client).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(storyId), startCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> beforeCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(beforeSuiteId), beforeCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> scenarioCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(scenarioId), scenarioCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		StartTestItemRQ beforeStorySuiteStart = startItems.get(0);
		assertThat(beforeStorySuiteStart.getName(), equalTo(LIFECYCLE_SUITE_NAME));
		String beforeSuiteCodeRef = STORY_PATH + String.format(LIFECYCLE_PATTERN, LIFECYCLE_SUITE_NAME);
		assertThat(beforeStorySuiteStart.getCodeRef(), equalTo(beforeSuiteCodeRef));

		StartTestItemRQ beforeStoryStart = beforeCaptor.getValue();
		assertThat(beforeStoryStart.getName(), equalTo(LIFECYCLE_STEP_NAME));
		assertThat(beforeStoryStart.getCodeRef(),
				equalTo(beforeSuiteCodeRef + String.format("/[BEFORE_SUITE:%s]", LIFECYCLE_STEP_NAME))
		);
		assertThat(beforeStoryStart.getType(), equalTo(ItemType.BEFORE_SUITE.name()));

		String scenarioCodeRef = STORY_PATH + String.format(SCENARIO_PATTERN, SCENARIO_NAME);
		StartTestItemRQ scenarioStart = startItems.get(1);
		assertThat(scenarioStart.getName(), equalTo(SCENARIO_NAME));
		assertThat(scenarioStart.getCodeRef(), equalTo(scenarioCodeRef));
		assertThat(scenarioStart.getType(), equalTo(ItemType.SCENARIO.name()));

		StartTestItemRQ step = scenarioCaptor.getValue();
		String stepCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, STEP_NAME);
		assertThat(step.getName(), equalTo(STEP_NAME));
		assertThat(step.getCodeRef(), equalTo(stepCodeRef));
		assertThat(step.getType(), equalTo(ItemType.STEP.name()));

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(beforeStepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(stepId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishStepCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishStepCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishStepCaptor.getAllValues();
		finishItems.forEach(i -> assertThat(i.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
