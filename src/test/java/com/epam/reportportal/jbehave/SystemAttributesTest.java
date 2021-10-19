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
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SystemAttributesTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			testExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepId);
		mockBatchLogging(client);
	}

	private static final String STORY_NAME = "NoScenario.story";
	private static final String STORY_PATH = "stories/" + STORY_NAME;

	@Test
	public void verify_start_launch_request_contains_system_attributes() {
		run(format, STORY_PATH, new EmptySteps());

		ArgumentCaptor<StartLaunchRQ> startCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(client).startLaunch(startCaptor.capture());

		StartLaunchRQ launchStart = startCaptor.getValue();

		Set<ItemAttributesRQ> attributes = launchStart.getAttributes();
		assertThat(attributes, allOf(notNullValue(), hasSize(greaterThan(0))));
		Set<String> attributesStr = attributes.stream()
				.filter(ItemAttributesRQ::isSystem)
				.map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.toSet());
		assertThat(attributesStr, hasSize(4));
		assertThat(attributesStr, hasItem("skippedIssue:true"));
		assertThat(attributesStr, hasItem("agent:test-name|test-version"));
		assertThat(attributesStr, hasItem(startsWith("os:")));
		assertThat(attributesStr, hasItem(startsWith("jvm:")));
	}
}
