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

package com.epam.reportportal.jbehave.steps;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.CompositeSteps;
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.jbehave.integration.basic.FailedSteps;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class CompositeStepFailureTest extends BaseTest {
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(3).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			Executors.newSingleThreadExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepIds);
		mockBatchLogging(client);
	}

	private static final String STORY = "stories/composite/CompositeStepFailure.story";

	private static final List<String> STEP_NAMES = Arrays.asList("Given failed composite step",
			"Given I have a failed step",
			"Then I have another empty step"
	);

	@Test
	public void verify_story_with_a_failed_composite_step() {
		run(format, STORY, new CompositeSteps(), new EmptySteps(), new FailedSteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(any());
		verify(client).startTestItem(same(storyId), any());
		verify(client, times(stepIds.size())).startTestItem(same(scenarioId), startCaptor.capture());

		List<StartTestItemRQ> startRequests = startCaptor.getAllValues();

		IntStream.range(0, startRequests.size()).forEach(i -> {
			StartTestItemRQ rq = startRequests.get(i);
			assertThat(rq.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
		});

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		stepIds.forEach(id -> verify(client).finishTestItem(same(id), finishCaptor.capture()));

		List<FinishTestItemRQ> finishRequests = finishCaptor.getAllValues();
		assertThat(finishRequests.get(0).getStatus(), equalTo(ItemStatus.PASSED.name()));
		assertThat(finishRequests.get(1).getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishRequests.get(2).getStatus(), equalTo(ItemStatus.SKIPPED.name()));
	}
}
