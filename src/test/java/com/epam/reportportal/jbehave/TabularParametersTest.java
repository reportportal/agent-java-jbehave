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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TabularParametersTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final String stepId = CommonUtils.namedId("step_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client, standardParameters()));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, stepId);
		mockBatchLogging(client);
	}

	private static final String STORY_NAME = "TabularParameters.story";
	private static final String STORY_PATH = "stories/" + STORY_NAME;

	@Test
	public void verify_a_step_with_tabular_parameters() {
		run(format, STORY_PATH, new StockSteps());

		verify(client).startTestItem(any());
		verify(client).startTestItem(same(storyId), any());
		verify(client).startTestItem(same(scenarioId), any());

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(stepId), finishCaptor.capture());
		verify(client).finishTestItem(same(scenarioId), finishCaptor.capture());
		verify(client).finishTestItem(same(storyId), finishCaptor.capture());

		// Finish items verification
		finishCaptor.getAllValues().forEach(f -> assertThat(f.getStatus(), equalTo(ItemStatus.PASSED.name())));
	}
}
