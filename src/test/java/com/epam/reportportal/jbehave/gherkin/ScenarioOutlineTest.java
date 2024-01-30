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
import com.epam.reportportal.utils.markdown.MarkdownUtils;
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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ScenarioOutlineTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final List<String> scenarioIds = Stream.generate(() -> CommonUtils.namedId("scenario_")).limit(3).collect(Collectors.toList());
	private final List<Pair<String, List<String>>> stepIds = scenarioIds.stream()
			.map(e -> Pair.of(e, Stream.generate(() -> CommonUtils.namedId("step_")).limit(3).collect(Collectors.toList())))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, stepIds);
		mockBatchLogging(client);

	}

	private static final String EXAMPLE_NAME = "Test with different parameters";
	//@formatter:off
	private static final String PARAMETERS_PREFIX =
			"Parameters:\n\n"
					+ MarkdownUtils.TABLE_INDENT + "|\u00A0\u00A0\u00A0str\u00A0\u00A0\u00A0|\u00A0parameters\u00A0|\n"
					+ MarkdownUtils.TABLE_INDENT + "|---------|------------|\n"
					+ MarkdownUtils.TABLE_INDENT;
	//@formatter:on
	private static final List<String> EXAMPLE_DESCRIPTIONS = asList(
			PARAMETERS_PREFIX
					+ "|\u00A0\"first\"\u00A0|\u00A0\u00A0\u00A0\u00A0123\u00A0\u00A0\u00A0\u00A0\u00A0|",
			PARAMETERS_PREFIX
					+ "|\u00A0\"scond\"\u00A0|\u00A0\u00A0\u00A0\u00A0321\u00A0\u00A0\u00A0\u00A0\u00A0|",
			PARAMETERS_PREFIX
					+ "|\u00A0\"third\"\u00A0|\u00A0\u00A0\u00A0\u00A0213\u00A0\u00A0\u00A0\u00A0\u00A0|"
	);

	private static final List<Map<String, String>> OUTLINE_PARAMETERS = Arrays.asList(new HashMap<String, String>() {{
		put("str", "\"first\"");
		put("parameters", "123");
	}}, new HashMap<String, String>() {{
		put("str", "\"scond\"");
		put("parameters", "321");
	}}, new HashMap<String, String>() {{
		put("str", "\"third\"");
		put("parameters", "213");
	}});

	private static final List<String> STEP_NAMES = Arrays.asList("Given It is test with parameters",
			"When I have parameter \"first\"",
			"Then I emit number 123 on level info",
			"Given It is test with parameters",
			"When I have parameter \"scond\"",
			"Then I emit number 321 on level info",
			"Given It is test with parameters",
			"When I have parameter \"third\"",
			"Then I emit number 213 on level info"
	);

	private static final List<Map<String, String>> STEP_PARAMETERS = Arrays.asList(new HashMap<>(), new HashMap<String, String>() {{
		put("str", "\"first\"");
	}}, new HashMap<String, String>() {{
		put("parameters", "123");
	}}, new HashMap<>(), new HashMap<String, String>() {{
		put("str", "\"scond\"");
	}}, new HashMap<String, String>() {{
		put("parameters", "321");
	}}, new HashMap<>(), new HashMap<String, String>() {{
		put("str", "\"third\"");
	}}, new HashMap<String, String>() {{
		put("parameters", "213");
	}});

	@Test
	public void verify_story_with_scenario_outline_names_types_and_parameters() {
		run(format, Collections.singletonList("features/ScenarioOutline.feature"), new GherkinStoryParser(), new ParameterizedSteps());

		verify(client, times(1)).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> exampleCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(3)).startTestItem(same(storyId), exampleCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(3)).startTestItem(same(scenarioIds.get(0)), stepCaptor.capture());
		verify(client, times(3)).startTestItem(same(scenarioIds.get(1)), stepCaptor.capture());
		verify(client, times(3)).startTestItem(same(scenarioIds.get(2)), stepCaptor.capture());

		// Start items verification
		List<StartTestItemRQ> examples = exampleCaptor.getAllValues();
		IntStream.range(0, examples.size()).forEach(i -> {
			StartTestItemRQ rq = examples.get(i);
			assertThat(rq.getName(), equalTo(EXAMPLE_NAME));
			assertThat(rq.getDescription(), equalTo(EXAMPLE_DESCRIPTIONS.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.TEST.name()));
			assertThat(rq.getParameters(), hasSize(OUTLINE_PARAMETERS.get(i).size()));
			rq.getParameters().forEach(p -> assertThat(OUTLINE_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});

		List<StartTestItemRQ> steps = stepCaptor.getAllValues();
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ rq = steps.get(i);
			assertThat(rq.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
			assertThat(rq.getParameters(), hasSize(STEP_PARAMETERS.get(i).size()));
			rq.getParameters().forEach(p -> assertThat(STEP_PARAMETERS.get(i), hasEntry(p.getKey(), p.getValue())));
		});
	}
}
