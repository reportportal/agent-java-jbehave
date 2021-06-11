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
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class TwoScenarioTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final List<String> scenarioIds = Stream.generate(() -> CommonUtils.namedId("scenario_")).limit(2).collect(Collectors.toList());
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("test_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, List<String>>> tests = Arrays.asList(
			Pair.of(scenarioIds.get(0), Collections.singletonList(stepIds.get(0))),
			Pair.of(scenarioIds.get(1), Collections.singletonList(stepIds.get(1)))
	);

	private final ListenerParameters params = standardParameters();
	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final ReportPortal reportPortal = ReportPortal.create(client, params, executorService);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(reportPortal);

	@BeforeEach
	public void setup() {
		mockLaunch(client, null, storyId, tests);
		mockBatchLogging(client);
	}

	private static final String STORY_PATH = "stories/TwoScenarios.story";

	@Test
	public void two_scenarios_reporting_test() {
		run(format, STORY_PATH, new EmptySteps());

		ArgumentCaptor<FinishTestItemRQ> finishRqCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);

		verify(client, times(1)).startTestItem(any());
		verify(client, times(2)).startTestItem(same(storyId), any());
		verify(client, times(1)).startTestItem(same(scenarioIds.get(0)), any());
		verify(client, times(1)).startTestItem(same(scenarioIds.get(1)), any());
		verify(client, times(1)).finishTestItem(same(stepIds.get(0)), finishRqCaptor.capture());
		verify(client, times(1)).finishTestItem(same(stepIds.get(1)), finishRqCaptor.capture());
		verify(client, times(1)).finishTestItem(same(scenarioIds.get(0)), finishRqCaptor.capture());
		verify(client, times(1)).finishTestItem(same(scenarioIds.get(1)), finishRqCaptor.capture());
		verify(client, times(1)).finishTestItem(same(storyId), finishRqCaptor.capture());

		finishRqCaptor.getAllValues().forEach(rq -> assertThat(rq.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
