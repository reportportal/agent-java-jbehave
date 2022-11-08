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

import com.epam.reportportal.jbehave.integration.basic.CallbackReportingSteps;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;

public class CallbackReportingTest extends BaseTest {
	private final String launchId = CommonUtils.namedId("launch_");
	private final String suiteId = CommonUtils.namedId("suite_");
	private final List<String> scenarioIds = Stream.generate(() -> CommonUtils.namedId("scenario_")).limit(2).collect(Collectors.toList());
	private final List<String> stepIds = Stream.generate(() -> CommonUtils.namedId("test_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, List<String>>> tests = Arrays.asList(
			Pair.of(scenarioIds.get(0), Collections.singletonList(stepIds.get(0))),
			Pair.of(scenarioIds.get(1), Collections.singletonList(stepIds.get(1)))
	);

	private final ListenerParameters params = standardParameters();
	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ExecutorService executorService = testExecutor();
	private final ReportPortal reportPortal = ReportPortal.create(client, params, executorService);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(reportPortal);

	@BeforeEach
	public void setup() {
		mockLaunch(client, launchId, suiteId, tests);
		when(client.log(any(SaveLogRQ.class))).thenReturn(Maybe.just(new EntryCreatedAsyncRS()));
	}

	private static final String STORY_PATH = "stories/CallbackReportingScenario.story";

	@Test
	public void callback_reporting_test() {
		run(format, STORY_PATH, new CallbackReportingSteps());

		ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<FinishTestItemRQ> rqCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		if(IS_JBEHAVE_5) {
			verify(client, times(9)).finishTestItem(idCaptor.capture(), rqCaptor.capture());
		} else {
			verify(client, times(7)).finishTestItem(idCaptor.capture(), rqCaptor.capture());
		}

		ArgumentCaptor<SaveLogRQ> saveLogRQArgumentCaptor = ArgumentCaptor.forClass(SaveLogRQ.class);
		verify(client, times(1)).log(saveLogRQArgumentCaptor.capture());

		List<String> finishIds = idCaptor.getAllValues();
		List<FinishTestItemRQ> finishRqs = rqCaptor.getAllValues();

		List<Pair<String, FinishTestItemRQ>> idRqs = IntStream.range(0, finishIds.size())
				.mapToObj(i -> Pair.of(finishIds.get(i), finishRqs.get(i)))
				.collect(Collectors.toList());

		List<Pair<String, FinishTestItemRQ>> firstScenarioIds = idRqs.stream()
				.filter(e -> stepIds.get(0).equals(e.getKey()))
				.collect(Collectors.toList());

		List<Pair<String, FinishTestItemRQ>> secondScenarioIds = idRqs.stream()
				.filter(e -> stepIds.get(1).equals(e.getKey()))
				.collect(Collectors.toList());
		if(IS_JBEHAVE_5) {
			assertThat(firstScenarioIds, hasSize(3));
			assertThat(secondScenarioIds, hasSize(3));
		} else {
			assertThat(firstScenarioIds, hasSize(2));
			assertThat(secondScenarioIds, hasSize(2));
		}

		List<Pair<String, FinishTestItemRQ>> failureUpdates = firstScenarioIds.stream()
				.filter(r -> "FAILED".equals(r.getValue().getStatus()))
				.collect(Collectors.toList());
		assertThat(failureUpdates, hasSize(1));

		SaveLogRQ logRq = saveLogRQArgumentCaptor.getValue();
		assertThat(logRq.getItemUuid(), equalTo(failureUpdates.get(0).getKey()));

		secondScenarioIds.forEach(e -> assertThat(e.getValue().getStatus(), equalTo("PASSED")));
	}
}
