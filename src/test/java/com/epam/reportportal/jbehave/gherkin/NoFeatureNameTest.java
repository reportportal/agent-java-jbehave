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
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.jbehave.core.parsers.gherkin.GherkinStoryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class NoFeatureNameTest extends BaseTest {
	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(2).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(
			client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepIds);
		mockBatchLogging(client);
	}

	private static final String SIMPLE_GHERKIN_STORY = "features/NoFeatureName.feature";

	@Test
	public void verify_code_reference_generation_gherkin_feature() {
		run(format, Collections.singletonList(SIMPLE_GHERKIN_STORY), new GherkinStoryParser(), new EmptySteps());

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(storyId), any(StartTestItemRQ.class));
		verify(client, times(1)).startTestItem(same(scenarioId), any(StartTestItemRQ.class));

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(1));

		StartTestItemRQ storyRq = items.get(0);

		String storyCodeRef = SIMPLE_GHERKIN_STORY;
		assertThat(storyRq.getCodeRef(), allOf(notNullValue(), equalTo(storyCodeRef)));
		assertThat(storyRq.getType(), allOf(notNullValue(), equalTo(ItemType.STORY.name())));
		assertThat(storyRq.getName(), allOf(notNullValue(), equalTo(storyCodeRef.substring(storyCodeRef.lastIndexOf('/') + 1))));
		assertThat(storyRq.getDescription(), emptyString());
	}
}
