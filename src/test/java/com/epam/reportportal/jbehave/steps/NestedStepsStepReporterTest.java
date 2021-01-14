/*
 * Copyright 2020 EPAM Systems
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
import com.epam.reportportal.jbehave.integration.basic.NestedStepsStepReporterSteps;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class NestedStepsStepReporterTest extends BaseTest {

	private final String launchId = CommonUtils.namedId("launch_");
	private final String suiteId = CommonUtils.namedId("suite_");
	private final String testId = CommonUtils.namedId("test_");
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("step_")).limit(2).collect(Collectors.toList());

	// Step reporter
	private final List<String> stepNestedStepIds = Stream.generate(() -> CommonUtils.namedId("step_"))
			.limit(3)
			.collect(Collectors.toList());
	private final List<Pair<String, String>> stepNestedSteps = Stream.concat(
			Stream.of(Pair.of(stepIds.get(0), stepNestedStepIds.get(0))),
			stepNestedStepIds.stream().skip(1).map(s -> Pair.of(stepIds.get(1), s))
	).collect(Collectors.toList());

	private final ListenerParameters params = standardParameters();
	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final ReportPortal reportPortal = ReportPortal.create(client, params, executorService);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(reportPortal);

	@BeforeEach
	public void setup() {
		mockLaunch(client, launchId, suiteId, testId, stepIds);
		mockNestedSteps(client, stepNestedSteps);
		mockBatchLogging(client);
	}

	private static void verifyStepStart(StartTestItemRQ step, String stepName) {
		assertThat(step.getName(), equalTo(stepName));
		assertThat(step.isHasStats(), equalTo(Boolean.FALSE));
		assertThat(step.getType(), equalTo("STEP"));
	}

	private static void verifyLogEntry(SaveLogRQ firstStepLog, String stepId, String duringSecondNestedStepLog) {
		assertThat(firstStepLog.getItemUuid(), equalTo(stepId));
		assertThat(firstStepLog.getMessage(), containsString(duringSecondNestedStepLog));
		assertThat(firstStepLog.getFile(), nullValue());
	}

	private static final String STORY_PATH = "stories/ManualStepReporter.story";

	@Test
	@SuppressWarnings("unchecked")
	public void verify_step_reporter_steps_integrity() {
		run(format, STORY_PATH, new NestedStepsStepReporterSteps());

		verify(client, times(2)).startTestItem(same(testId), any());
		ArgumentCaptor<StartTestItemRQ> firstStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(stepIds.get(0)), firstStepCaptor.capture());
		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(client, atLeastOnce()).log(logCaptor.capture());
		StartTestItemRQ firstStep = firstStepCaptor.getValue();
		List<SaveLogRQ> logs = logCaptor.getAllValues()
				.stream()
				.flatMap(l -> l.getSerializedRQs().stream())
				.flatMap(l -> ((List<SaveLogRQ>) l.getRequest()).stream())
				.filter(rq-> !LogLevel.DEBUG.name().equals(rq.getLevel()))
				.collect(Collectors.toList());
		SaveLogRQ firstStepLog = logs.get(0);

		verifyStepStart(firstStep, NestedStepsStepReporterSteps.FIRST_NAME);
		verifyLogEntry(firstStepLog, stepNestedStepIds.get(0), NestedStepsStepReporterSteps.FIRST_NESTED_STEP_LOG);

		ArgumentCaptor<StartTestItemRQ> secondStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(stepIds.get(1)), secondStepCaptor.capture());
		List<StartTestItemRQ> secondSteps = secondStepCaptor.getAllValues();
		List<SaveLogRQ> secondStepLogs = logs.subList(1, logs.size());

		StartTestItemRQ secondStep = secondSteps.get(0);
		verifyStepStart(secondStep, NestedStepsStepReporterSteps.SECOND_NAME);
		verifyLogEntry(secondStepLogs.get(0), stepNestedStepIds.get(1), NestedStepsStepReporterSteps.DURING_SECOND_NESTED_STEP_LOG);
		verifyLogEntry(secondStepLogs.get(1), stepNestedStepIds.get(1), NestedStepsStepReporterSteps.SECOND_NESTED_STEP_LOG);

		StartTestItemRQ thirdStep = secondSteps.get(1);
		verifyStepStart(thirdStep, NestedStepsStepReporterSteps.THIRD_NAME);

		SaveLogRQ pugLog = secondStepLogs.get(2);
		assertThat(pugLog.getItemUuid(), equalTo(stepNestedStepIds.get(2)));
		assertThat(pugLog.getMessage(), emptyString());
		assertThat(pugLog.getFile(), notNullValue());

		verifyLogEntry(secondStepLogs.get(3), stepNestedStepIds.get(2), NestedStepsStepReporterSteps.THIRD_NESTED_STEP_LOG);

		ArgumentCaptor<String> finishIdCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> finishRqCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(7)).finishTestItem(finishIdCaptor.capture(), finishRqCaptor.capture());
		List<String> finishIds = finishIdCaptor.getAllValues();
		List<FinishTestItemRQ> finishRqs = finishRqCaptor.getAllValues();
		List<FinishTestItemRQ> nestedStepFinishes = IntStream.range(0, finishIds.size())
				.filter(i -> stepNestedStepIds.contains(finishIds.get(i)))
				.mapToObj(finishRqs::get)
				.collect(Collectors.toList());

		assertThat(nestedStepFinishes.get(0).getStatus(), equalTo("PASSED"));
		assertThat(nestedStepFinishes.get(1).getStatus(), equalTo("PASSED"));
		assertThat(nestedStepFinishes.get(2).getStatus(), equalTo("FAILED"));

		List<FinishTestItemRQ> stepFinishes = IntStream.range(0, finishIds.size())
				.filter(i -> !stepNestedStepIds.contains(finishIds.get(i)))
				.mapToObj(finishRqs::get)
				.collect(Collectors.toList());

		assertThat(stepFinishes.get(0).getStatus(), equalTo("PASSED"));
		assertThat(stepFinishes.get(1).getStatus(), equalTo("FAILED"));
		assertThat(stepFinishes.get(2).getStatus(), equalTo("FAILED"));
		assertThat(stepFinishes.get(3).getStatus(), equalTo("FAILED"));
	}
}
