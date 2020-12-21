/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.integration.feature.StockExamplesSteps;
import com.epam.reportportal.jbehave.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class ExamplesCodeRefTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, String>> stepIds = exampleIds.stream()
			.flatMap(e -> Stream.generate(() -> Pair.of(e, CommonUtils.namedId("step_"))).limit(3))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private ReportPortalStepFormat format;

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, storyId, scenarioId, exampleIds);
		TestUtils.mockNestedSteps(client, stepIds);
		TestUtils.mockBatchLogging(client);
		format = new ReportPortalStepFormat(ReportPortal.create(client, TestUtils.standardParameters()));
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

	private static final List<String> STEP_NAMES = Arrays.asList(
			"Given a stock of symbol STK1 and a threshold 10.0",
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
	public void verify_story_with_examples() {
		TestUtils.run(format, "stories/Examples.story", new StockExamplesSteps());

		verify(client, times(1)).startTestItem(any());
		verify(client, times(1)).startTestItem(same(storyId), any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(scenarioId), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(1)), startCaptor.capture());

		// TODO: finish this test
	}
}
