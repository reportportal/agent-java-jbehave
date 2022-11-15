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

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NoScenarioTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepId);
		mockBatchLogging(client);
	}

	private static final String STORY_NAME = "NoScenario.story";
	private static final String STORY_PATH = "stories/" + STORY_NAME;
	private static final String DEFAULT_SCENARIO_NAME = "No name";
	private static final String STEP_NAME = "Given I have empty step";

	@Test
	public void verify_a_story_without_scenario() {
		run(format, STORY_PATH, new EmptySteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(startCaptor.capture());
		verify(client).startTestItem(same(storyId), startCaptor.capture());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(stepId), finishCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		StartTestItemRQ startStory = startItems.get(0);
		assertThat(startStory.getName(), equalTo(STORY_NAME));
		assertThat(startStory.getCodeRef(), equalTo(STORY_PATH));

		StartTestItemRQ startScenario = startItems.get(1);
		String scenarioCodeRef = STORY_PATH + String.format(SCENARIO_PATTERN, DEFAULT_SCENARIO_NAME);
		assertThat(startScenario.getName(), equalTo(DEFAULT_SCENARIO_NAME));
		assertThat(startScenario.getCodeRef(), equalTo(scenarioCodeRef));

		StartTestItemRQ startStep = startItems.get(2);
		String stepCodeRef = scenarioCodeRef + String.format(STEP_PATTERN, STEP_NAME);
		assertThat(startStep.getName(), equalTo(STEP_NAME));
		assertThat(startStep.getCodeRef(), equalTo(stepCodeRef));

		// Finish items verification
		List<FinishTestItemRQ> finishItems = finishCaptor.getAllValues();
		FinishTestItemRQ stepFinish = finishItems.get(0);
		assertThat(stepFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ scenarioFinish = finishItems.get(1);
		assertThat(scenarioFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));

		FinishTestItemRQ storyFinish = finishItems.get(2);
		assertThat(storyFinish.getStatus(), equalTo(ItemStatus.PASSED.name()));
	}
}
