package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.integration.basic.EmptySteps;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MetaTest extends BaseTest {

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

	private static final String STORY_NAME = "MetaInfo.story";
	private static final String STORY_PATH = "stories/" + STORY_NAME;
	private static final Set<Pair<String, String>> SUITE_ATTRIBUTES = new HashSet<Pair<String, String>>() {{
		add(Pair.of("author", "Mauro"));
		add(Pair.of("themes", "UI Usability"));
		add(Pair.of(null, "smoke"));
	}};

	private static final Set<Pair<String, String>> SCENARIO_ATTRIBUTES = new HashSet<Pair<String, String>>() {{
		add(Pair.of("ignored", "false"));
	}};

	@Test
	public void verify_a_story_with_meta_attributes() {
		run(format, STORY_PATH, new EmptySteps());

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client).startTestItem(startCaptor.capture());
		verify(client).startTestItem(same(storyId), startCaptor.capture());
		verify(client).startTestItem(same(scenarioId), any());

		List<StartTestItemRQ> items = startCaptor.getAllValues();

		StartTestItemRQ startSuite = items.get(0);
		assertThat(startSuite.getAttributes(), allOf(notNullValue(), hasSize(SUITE_ATTRIBUTES.size())));
		Set<ItemAttributesRQ> suiteAttributes = startSuite.getAttributes();
		suiteAttributes.forEach(a -> assertThat(SUITE_ATTRIBUTES, hasItem(Pair.of(a.getKey(), a.getValue()))));

		StartTestItemRQ startScenario = items.get(1);
		assertThat(startScenario.getAttributes(), allOf(notNullValue(), hasSize(SCENARIO_ATTRIBUTES.size())));
		Set<ItemAttributesRQ> scenarioAttributes = startScenario.getAttributes();
		scenarioAttributes.forEach(a -> assertThat(SCENARIO_ATTRIBUTES, hasItem(Pair.of(a.getKey(), a.getValue()))));
	}

}