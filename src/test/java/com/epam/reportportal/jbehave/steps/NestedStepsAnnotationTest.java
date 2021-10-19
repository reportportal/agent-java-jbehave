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
import com.epam.reportportal.jbehave.integration.basic.NestedStepsAnnotationSteps;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class NestedStepsAnnotationTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Arrays.asList(CommonUtils.namedId("step_"), CommonUtils.namedId("step_"));
	private final List<String> nestedStepIds = Stream.generate(() -> CommonUtils.namedId("nested_step_"))
			.limit(3)
			.collect(Collectors.toList());
	private final String nestedNestedStepId = CommonUtils.namedId("double_nested_step_");
	private final List<Pair<String, String>> firstLevelNestedStepIds = Stream.concat(Stream.of(Pair.of(stepIds.get(0),
					nestedStepIds.get(0)
			)), nestedStepIds.stream().skip(1).map(i -> Pair.of(stepIds.get(1), i)))
			.collect(Collectors.toList());

	private final ListenerParameters params = standardParameters();
	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ExecutorService executorService = testExecutor();
	private final ReportPortal reportPortal = ReportPortal.create(client, params, executorService);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(reportPortal);

	@BeforeEach
	public void setup() {
		mockLaunch(client, null, storyId, scenarioId, stepIds);
		mockNestedSteps(client, firstLevelNestedStepIds);
		mockNestedSteps(client, Pair.of(nestedStepIds.get(0), nestedNestedStepId));
		mockBatchLogging(client);
	}

	public static final List<String> FIRST_LEVEL_NAMES = Arrays.asList("A step inside step",
			"A step with parameters",
			"A step with attributes"
	);

	private static final String STORY_PATH = "stories/AnnotationNestedSteps.story";

	@Test
	public void test_annotation_based_nested_steps() {
		run(format, STORY_PATH, new NestedStepsAnnotationSteps());

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(storyId), captor.capture());
		verify(client, times(2)).startTestItem(same(scenarioId), captor.capture());
		List<StartTestItemRQ> parentItems = captor.getAllValues();
		parentItems.forEach(i -> assertThat(i.isHasStats(), anyOf(equalTo(Boolean.TRUE))));

		ArgumentCaptor<StartTestItemRQ> firstLevelCaptor1 = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(same(stepIds.get(0)), firstLevelCaptor1.capture());

		StartTestItemRQ firstLevelRq1 = firstLevelCaptor1.getValue();
		assertThat(firstLevelRq1.getName(), equalTo(FIRST_LEVEL_NAMES.get(0)));
		assertThat(firstLevelRq1.isHasStats(), equalTo(Boolean.FALSE));

		ArgumentCaptor<StartTestItemRQ> firstLevelCaptor2 = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(stepIds.get(1)), firstLevelCaptor2.capture());

		List<StartTestItemRQ> firstLevelRqs2 = firstLevelCaptor2.getAllValues();
		IntStream.range(1, FIRST_LEVEL_NAMES.size()).forEach(i -> {
			assertThat(firstLevelRqs2.get(i - 1).getName(), equalTo(FIRST_LEVEL_NAMES.get(i)));
			assertThat(firstLevelRqs2.get(i - 1).isHasStats(), equalTo(Boolean.FALSE));
		});

		ArgumentCaptor<StartTestItemRQ> secondLevelCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(nestedStepIds.get(0)), secondLevelCaptor.capture());

		StartTestItemRQ thirdLevelRq = secondLevelCaptor.getValue();
		assertThat(thirdLevelRq.getName(), equalTo("A step inside nested step"));
		assertThat(thirdLevelRq.isHasStats(), equalTo(Boolean.FALSE));

		// Attributes verification
		StartTestItemRQ stepWithAttributes = firstLevelRqs2.get(1);
		Set<ItemAttributesRQ> attributes = stepWithAttributes.getAttributes();
		assertThat(attributes, allOf(notNullValue(), hasSize(2)));
		List<Pair<String, String>> kvAttributes = attributes.stream()
				.map(a -> Pair.of(a.getKey(), a.getValue()))
				.collect(Collectors.toList());
		List<Pair<String, String>> keyAndValueList = kvAttributes.stream().filter(kv -> kv.getKey() != null).collect(Collectors.toList());
		assertThat(keyAndValueList, hasSize(1));
		assertThat(keyAndValueList.get(0).getKey(), equalTo("key"));
		assertThat(keyAndValueList.get(0).getValue(), equalTo("value"));

		List<Pair<String, String>> tagList = kvAttributes.stream().filter(kv -> kv.getKey() == null).collect(Collectors.toList());
		assertThat(tagList, hasSize(1));
		assertThat(tagList.get(0).getValue(), equalTo("tag"));
	}
}
