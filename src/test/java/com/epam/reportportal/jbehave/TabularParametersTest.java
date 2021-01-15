/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.integration.basic.StockSteps;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TabularParametersTest extends BaseTest {

	public static final String SCENARIO_PATTERN = "/[SCENARIO:%s]";
	public static final String STEP_PATTERN = "/[STEP:%s]";

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			Executors.newSingleThreadExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepId);
		mockBatchLogging(client);
	}

	private static final String STORY_NAME = "TabularParameters.story";
	private static final String STORY_PATH = "stories/" + STORY_NAME;
	private static final String STEP_NAME = "Given the traders:\n|name|rank|\n|Larry|Stooge 3|\n|Moe|Stooge 1|\n|Curly|Stooge 2|";
	private static final String CODE_REF = STEP_NAME.replace("\n", "");

	@Test
	public void verify_a_step_with_tabular_parameters() {
		run(format, STORY_PATH, new StockSteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(any());
		verify(client).startTestItem(same(storyId), any());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());

		assertThat(startCaptor.getValue().getName(), equalTo(STEP_NAME));
		assertThat(startCaptor.getValue().getCodeRef(),
				equalTo(STORY_PATH + String.format(SCENARIO_PATTERN, "Tabular parameters") + String.format(STEP_PATTERN, CODE_REF))
		);

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(stepId), finishCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishCaptor.capture());

		// Finish items verification
		finishCaptor.getAllValues().forEach(f -> assertThat(f.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
