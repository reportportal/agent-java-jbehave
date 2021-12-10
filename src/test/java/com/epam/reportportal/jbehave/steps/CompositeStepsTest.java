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
import com.epam.reportportal.jbehave.integration.basic.ParameterizedSteps;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class CompositeStepsTest extends BaseTest {
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(1).collect(
			Collectors.toList());

	private final List<Pair<String, String>> stepIds = exampleIds.stream().flatMap(
			e -> Stream.generate(() -> Pair.of(e, CommonUtils.namedId("step_"))).limit(6)).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, exampleIds);
		mockNestedSteps(client, stepIds);
		mockBatchLogging(client);
	}

	private static final String STORY = "stories/composite/CompositeSteps.story";

	private static final List<String> STEP_NAMES = Arrays.asList(
			"Given composite step",
			"Given I have empty step",
			"Then I have another empty step",
			"When parametrized with a string step",
			"When I have parameter <parameter>",
			"When I have a step with a string parameter <parameter>"
	);

	@Test
	public void verify_story_with_composite_steps_passed() {
		run(format, STORY, new CompositeSteps(), new EmptySteps(), new ParameterizedSteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(any());
		verify(client).startTestItem(same(storyId), any());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());
		verify(client, times(6)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		List<StartTestItemRQ> startRequests = startCaptor.getAllValues();
		startRequests.remove(0);
		IntStream.range(0, startRequests.size()).forEach(i -> {
			StartTestItemRQ rq = startRequests.get(i);
			assertThat(rq.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
		});

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		stepIds.forEach(id -> verify(client).finishTestItem(same(id.getValue()), finishCaptor.capture()));

		finishCaptor.getAllValues().forEach(rq -> assertThat(rq.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
