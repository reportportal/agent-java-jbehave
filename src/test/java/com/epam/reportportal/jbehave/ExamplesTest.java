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

import com.epam.reportportal.jbehave.integration.basic.ParameterizedSteps;
import com.epam.reportportal.jbehave.integration.basic.StockSteps;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.annotations.When;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ExamplesTest extends BaseTest {

	public static final int STEPS_QUANTITY = 4;
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, String>> stepIds = exampleIds.stream()
			.flatMap(e -> Stream.generate(() -> Pair.of(e, CommonUtils.namedId("step_"))).limit(STEPS_QUANTITY))
			.collect(Collectors.toList());

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

	private static final List<String> EXAMPLE_NAMES = asList(
			"Example: [symbol: STK1; threshold: 10.0; price: 5.0; status: OFF]",
			"Example: [symbol: STK1; threshold: 10.0; price: 11.0; status: ON]"
	);

	private static final List<Map<String, String>> EXAMPLE_PARAMETERS = asList(new HashMap<String, String>() {{
		put("symbol", "STK1");
		put("threshold", "10.0");
		put("price", "5.0");
		put("status", "OFF");
	}}, new HashMap<String, String>() {{
		put("symbol", "STK1");
		put("threshold", "10.0");
		put("price", "11.0");
		put("status", "ON");
	}});

	private static final List<String> STEP_NAMES = asList(
			"Given a stock of symbol STK1 and a threshold 10.0",
			"When the stock is traded at price 5.0",
			"Then the alert status should be status OFF",
			"When I have first parameter STK1 and second parameter STK1",
			"Given a stock of symbol STK1 and a threshold 10.0",
			"When the stock is traded at price 11.0",
			"Then the alert status should be status ON",
			"When I have first parameter STK1 and second parameter STK1"
	);

	private static final List<List<ParameterResource>> STEP_PARAMETERS = asList(
		asList(parameterOf("symbol", "STK1"), parameterOf("threshold", "10.0")),
		asList(parameterOf("price", "5.0")),
		asList(parameterOf("status", "OFF")),
		asList(parameterOf("symbol", "STK1"), parameterOf("symbol", "STK1")),
		asList(parameterOf("symbol", "STK1"), parameterOf("threshold", "10.0")),
		asList(parameterOf("price", "11.0")),
		asList(parameterOf("status", "ON")),
		asList(parameterOf("symbol", "STK1"), parameterOf("symbol", "STK1"))
	);


	@Test
	public void verify_story_with_examples_names_types_and_parameters() {
		run(format, "stories/Examples.story", new StockSteps(), new ParameterizedSteps());

		verify(client, times(1)).startTestItem(any());
		verify(client, times(1)).startTestItem(same(storyId), any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(scenarioId), startCaptor.capture());
		verify(client, times(STEPS_QUANTITY)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		verify(client, times(STEPS_QUANTITY)).startTestItem(same(exampleIds.get(1)), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		List<StartTestItemRQ> examples = startItems.subList(0, 2);
		IntStream.range(0, examples.size()).forEach(i -> {
			StartTestItemRQ rq = examples.get(i);
			assertThat(rq.getName(), equalTo(EXAMPLE_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.TEST.name()));
			assertThat(rq.getParameters(), hasSize(STEPS_QUANTITY));
			rq.getParameters().forEach(p -> assertThat(EXAMPLE_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});

		List<StartTestItemRQ> steps = startItems.subList(2, 2 + 2 * STEPS_QUANTITY);
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ rq = steps.get(i);
			assertThat(rq.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
			assertEquals(STEP_PARAMETERS.get(i), rq.getParameters());
		});

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		stepIds.forEach(s -> verify(client, times(1)).finishTestItem(same(s.getValue()), finishStepCaptor.capture()));

		List<FinishTestItemRQ> finishSteps = finishStepCaptor.getAllValues();
		assertThat(finishSteps, hasSize(2 * STEPS_QUANTITY));
		finishSteps.forEach(rq -> assertThat(rq.getStatus(), equalTo(ItemStatus.PASSED.name())));

		ArgumentCaptor<FinishTestItemRQ> finishExampleCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		exampleIds.forEach(s -> verify(client, times(1)).finishTestItem(same(s), finishExampleCaptor.capture()));
		finishExampleCaptor.getAllValues().forEach(rq -> assertThat(rq.getStatus(), equalTo(ItemStatus.PASSED.name())));

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(1)).finishTestItem(same(scenarioId), finishCaptor.capture());
		verify(client, times(1)).finishTestItem(same(storyId), finishCaptor.capture());
		finishCaptor.getAllValues().forEach(rq -> assertThat(rq.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
