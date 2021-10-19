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

package com.epam.reportportal.jbehave.gherkin;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.ParameterizedSteps;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.parsers.gherkin.GherkinStoryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ScenarioOutlineTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(3).collect(Collectors.toList());

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

	private static final List<String> EXAMPLE_NAMES = Arrays.asList("Example: [str: \"first\"; parameters: 123]",
			"Example: [str: \"second\"; parameters: 12345]",
			"Example: [str: \"third\"; parameters: 12345678]"
	);

	private static final List<Map<String, String>> OUTLINE_PARAMETERS = Arrays.asList(new HashMap<String, String>() {{
		put("str", "\"first\"");
		put("parameters", "123");
	}}, new HashMap<String, String>() {{
		put("str", "\"second\"");
		put("parameters", "12345");
	}}, new HashMap<String, String>() {{
		put("str", "\"third\"");
		put("parameters", "12345678");
	}});

	private static final List<String> STEP_NAMES = Arrays.asList("Given It is test with parameters",
			"When I have parameter \"first\"",
			"Then I emit number 123 on level info",
			"Given It is test with parameters",
			"When I have parameter \"second\"",
			"Then I emit number 12345 on level info",
			"Given It is test with parameters",
			"When I have parameter \"third\"",
			"Then I emit number 12345678 on level info"
	);

	private static final List<Map<String, String>> STEP_PARAMETERS = Arrays.asList(new HashMap<>(), new HashMap<String, String>() {{
		put("str", "\"first\"");
	}}, new HashMap<String, String>() {{
		put("parameters", "123");
	}}, new HashMap<>(), new HashMap<String, String>() {{
		put("str", "\"second\"");
	}}, new HashMap<String, String>() {{
		put("parameters", "12345");
	}}, new HashMap<>(), new HashMap<String, String>() {{
		put("str", "\"third\"");
	}}, new HashMap<String, String>() {{
		put("parameters", "12345678");
	}});

	@Test
	public void verify_story_with_scenario_outline_names_types_and_parameters() {
		run(format, Collections.singletonList("features/ScenarioOutline.feature"), new GherkinStoryParser(), new ParameterizedSteps());

		verify(client, times(1)).startTestItem(any());
		verify(client, times(1)).startTestItem(same(storyId), any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(3)).startTestItem(same(scenarioId), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(1)), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(2)), startCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		List<StartTestItemRQ> examples = startItems.subList(0, 3);
		IntStream.range(0, examples.size()).forEach(i -> {
			StartTestItemRQ rq = examples.get(i);
			assertThat(rq.getName(), equalTo(EXAMPLE_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.TEST.name()));
			assertThat(rq.getParameters(), hasSize(OUTLINE_PARAMETERS.get(i).size()));
			rq.getParameters().forEach(p -> assertThat(OUTLINE_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});

		List<StartTestItemRQ> steps = startItems.subList(3, 3 + 9);
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ rq = steps.get(i);
			assertThat(rq.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
			assertThat(rq.getParameters(), hasSize(STEP_PARAMETERS.get(i).size()));
			rq.getParameters().forEach(p -> assertThat(STEP_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});
	}
}
