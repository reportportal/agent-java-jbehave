package com.epam.reportportal.jbehave.id;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
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

public class TestCaseIdSimpleTest extends BaseTest {

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

	private static final String STORY_NAME = "NoScenario.story";
	private static final String STORY_PATH = "stories/" + STORY_NAME;
	private static final String DEFAULT_SCENARIO_NAME = "No name";
	private static final String STEP_NAME = "Given I have empty step";

	@Test
	public void verify_test_case_id_for_a_simple_scenario() {
		run(format, STORY_PATH, new EmptySteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(any());
		verify(client).startTestItem(same(storyId), any());
		verify(client).startTestItem(startCaptor.capture());
		verify(client).startTestItem(same(scenarioId), startCaptor.capture());

		// Start items verification
		StartTestItemRQ startStep = startCaptor.getValue();
		String stepCodeRef = STORY_PATH + String.format("/[SCENARIO:%s]", DEFAULT_SCENARIO_NAME) + String.format("/[STEP:%s]", STEP_NAME);
		assertThat(startStep.getTestCaseId(), equalTo(stepCodeRef));
	}
}
