/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.integration.basic.StockSteps;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ExamplesTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, String>> stepIds = exampleIds.stream()
			.flatMap(e -> Stream.generate(() -> Pair.of(e, CommonUtils.namedId("step_"))).limit(3))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			Executors.newSingleThreadExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, exampleIds);
		mockNestedSteps(client, stepIds);
		mockBatchLogging(client);

	}

	private static final List<String> EXAMPLE_NAMES = Arrays.asList(
			"Example: [symbol: STK1; threshold: 10.0; price: 5.0; status: OFF]",
			"Example: [symbol: STK1; threshold: 10.0; price: 11.0; status: ON]"
	);

	private static final List<Map<String, String>> EXAMPLE_PARAMETERS = Arrays.asList(new HashMap<String, String>() {{
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

	private static final List<String> STEP_NAMES = Arrays.asList("Given a stock of symbol STK1 and a threshold 10.0",
			"When the stock is traded at price 5.0",
			"Then the alert status should be status OFF",
			"Given a stock of symbol STK1 and a threshold 10.0",
			"When the stock is traded at price 11.0",
			"Then the alert status should be status ON"
	);

	private static final List<Map<String, String>> STEP_PARAMETERS = Arrays.asList(new HashMap<String, String>() {{
		put("symbol", "STK1");
		put("threshold", "10.0");
	}}, new HashMap<String, String>() {{
		put("price", "5.0");
	}}, new HashMap<String, String>() {{
		put("status", "OFF");
	}}, new HashMap<String, String>() {{
		put("symbol", "STK1");
		put("threshold", "10.0");
	}}, new HashMap<String, String>() {{
		put("price", "11.0");
	}}, new HashMap<String, String>() {{
		put("status", "ON");
	}});

	@Test
	public void verify_story_with_examples_names_types_and_parameters() {
		run(format, "stories/Examples.story", new StockSteps());

		verify(client, times(1)).startTestItem(any());
		verify(client, times(1)).startTestItem(same(storyId), any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(scenarioId), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(1)), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		List<StartTestItemRQ> examples = startItems.subList(0, 2);
		IntStream.range(0, examples.size()).forEach(i -> {
			StartTestItemRQ rq = examples.get(i);
			assertThat(rq.getName(), equalTo(EXAMPLE_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.TEST.name()));
			assertThat(rq.getParameters(), hasSize(4));
			rq.getParameters().forEach(p -> assertThat(EXAMPLE_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});

		List<StartTestItemRQ> steps = startItems.subList(2, 2 + 6);
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ rq = steps.get(i);
			assertThat(rq.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
			assertThat(rq.getParameters(), hasSize(STEP_PARAMETERS.get(i).size()));
			rq.getParameters().forEach(p -> assertThat(STEP_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});

		// Finish items verification
		ArgumentCaptor<FinishTestItemRQ> finishStepCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		stepIds.forEach(s -> verify(client, times(1)).finishTestItem(same(s.getValue()), finishStepCaptor.capture()));
		List<FinishTestItemRQ> finishSteps = finishStepCaptor.getAllValues();
		assertThat(finishSteps, hasSize(6));
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
