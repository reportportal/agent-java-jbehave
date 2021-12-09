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

package com.epam.reportportal.jbehave.coderef;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.StockSteps;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
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
import static org.mockito.Mockito.*;

public class ExamplesCodeRefTest extends BaseTest {

	public static final int STEPS_QUANTITY = 4;
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, String>> stepIds = exampleIds.stream()
			.flatMap(e -> Stream.generate(() -> Pair.of(e, CommonUtils.namedId("step_"))).limit(3))
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

	private static final List<String> EXAMPLE_NODES = Arrays.asList("[EXAMPLE:[symbol:STK1$;threshold:10.0;price:5.0;status:OFF]]",
			"[EXAMPLE:[symbol:STK1$;threshold:10.0;price:11.0;status:ON]]"
	);

	private static final List<String> STEP_NAMES = Arrays.asList("Given a stock of symbol <symbol> and a threshold <threshold>",
			"When the stock is traded at price <price>",
			"Then the alert status should be status <status>",
			"When I have first parameter <symbol> and second parameter <symbol>"
	);

	private static final String EXAMPLES_STORY = "stories/Examples.story";

	@Test
	public void verify_story_with_examples() {
		run(format, EXAMPLES_STORY, new StockSteps());

		verify(client, times(1)).startTestItem(any());
		verify(client, times(1)).startTestItem(same(storyId), any());
		ArgumentCaptor<StartTestItemRQ> exampleCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(scenarioId), exampleCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(STEPS_QUANTITY)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		verify(client, times(STEPS_QUANTITY)).startTestItem(same(exampleIds.get(1)), startCaptor.capture());

		String scenarioCodeRef = EXAMPLES_STORY + "/[SCENARIO:Stock trade alert]";

		List<StartTestItemRQ> items = startCaptor.getAllValues();
		List<StartTestItemRQ> examples = exampleCaptor.getAllValues();
		IntStream.range(0, examples.size()).forEach(i -> {
			StartTestItemRQ rq = examples.get(i);
			String exampleCodeRef = scenarioCodeRef + "/" + EXAMPLE_NODES.get(i);
			assertThat(rq.getCodeRef(), equalTo(exampleCodeRef));

			List<StartTestItemRQ> exampleSteps = items.subList(i * STEPS_QUANTITY, i * STEPS_QUANTITY + STEPS_QUANTITY);
			IntStream.range(0, exampleSteps.size()).forEach(j -> {
				StartTestItemRQ stepRq = exampleSteps.get(j);
				String stepCodeRef = exampleCodeRef + "/" + String.format("[STEP:%s]", STEP_NAMES.get(j));
				assertThat(stepRq.getCodeRef(), equalTo(stepCodeRef));
			});
		});
	}
}
