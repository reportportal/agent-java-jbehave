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

package com.epam.reportportal.jbehave.status;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import okhttp3.MultipartBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class ItemStatusSkippedTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(2).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepIds);
		mockBatchLogging(client);
	}

	private static final String SKIPPED_SCENARIO_PATH = "stories/status/SkippedScenario.story";

	@Test
	@SuppressWarnings("unchecked")
	public void verify_a_step_skipped_parent_status_calculated() {
		run(format, SKIPPED_SCENARIO_PATH);

		verify(client).startTestItem(any());
		verify(client).startTestItem(same(storyId), any());
		verify(client).startTestItem(same(scenarioId), any());

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(stepIds.get(0)), finishCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishCaptor.capture());

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(client, atLeast(1)).log(logCaptor.capture());

		List<FinishTestItemRQ> finishItems = finishCaptor.getAllValues();
		FinishTestItemRQ stepOneFinish = finishItems.get(0);
		assertThat(stepOneFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));

		verifyLogged(logCaptor, stepIds.get(0), LogLevel.WARN, "Unable to locate a step implementation");

		FinishTestItemRQ scenarioFinish = finishItems.get(1);
		assertThat(scenarioFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));

		FinishTestItemRQ storyFinish = finishItems.get(2);
		assertThat(storyFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
	}
}
