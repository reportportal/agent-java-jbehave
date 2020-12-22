/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.jbehave;

import com.epam.reportportal.jbehave.utils.TestUtils;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class GivenStoryCodeRefTest {

	private final String rootStoryId = CommonUtils.namedId("root_story_");
	private final String scenarioId = CommonUtils.namedId("root_scenario_");
	private final String stepId = CommonUtils.namedId("root_step_");

	private final String outerGivenStoryId = CommonUtils.namedId("outer_story_");
	private final String outerScenarioId = CommonUtils.namedId("outer_scenario_");
	private final List<String> outerStepIds = Stream.generate(() -> CommonUtils.namedId("outer_step_"))
			.limit(2)
			.collect(Collectors.toList());

	private final String innerGivenStoryId = CommonUtils.namedId("inner_story_");
	private final String innerScenarioId = CommonUtils.namedId("inner_scenario_");
	private final List<String> innerStepIds = Stream.generate(() -> CommonUtils.namedId("inner_step_"))
			.limit(2)
			.collect(Collectors.toList());

	private final List<Pair<String, List<String>>> stepIds = Arrays.asList(Pair.of(outerGivenStoryId,
			Collections.singletonList(outerScenarioId)
	), Pair.of(scenarioId, Arrays.asList(innerGivenStoryId, stepId)));

	private final List<Pair<String, String>> nestedStepIds = Stream.concat(
			Stream.concat(outerStepIds.stream().map(s -> Pair.of(outerScenarioId, s)),
					Stream.of(Pair.of(innerGivenStoryId, innerScenarioId))
			),
			innerStepIds.stream().map(s -> Pair.of(innerScenarioId, s))
	).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private ReportPortalStepFormat format;

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, rootStoryId, stepIds);
		TestUtils.mockNestedSteps(client, nestedStepIds);
		TestUtils.mockBatchLogging(client);
		format = new ReportPortalStepFormat(ReportPortal.create(client, TestUtils.standardParameters()));
	}

	private static final String GIVEN_STORIES_STORY = "stories/GivenStories.story";
	private static final List<String> DUMMY_SCENARIO_STEPS = Arrays.asList("Given I have empty step", "Then I have another empty step");
	private static final List<String> INLINE_PARAMETERS_STEPS = Arrays.asList("Given It is a step with an integer parameter 42",
			"When I have a step with a string parameter \"string\""
	);
	private static final String GIVEN_STORY_STEP = "When I have one more empty step";

	@Test
	public void verify_code_reference_generation_given_stories() {
		TestUtils.run(format, GIVEN_STORIES_STORY);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(2)).startTestItem(same(rootStoryId), captor.capture());
		verify(client, times(1)).startTestItem(same(outerGivenStoryId), captor.capture());
		verify(client, times(2)).startTestItem(same(outerScenarioId), captor.capture());
		verify(client, times(2)).startTestItem(same(scenarioId), captor.capture());
		verify(client, times(1)).startTestItem(same(innerGivenStoryId), captor.capture());
		verify(client, times(2)).startTestItem(same(innerScenarioId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(11));

		StartTestItemRQ rootItem = items.get(0);
		assertThat(rootItem.getCodeRef(), equalTo(GIVEN_STORIES_STORY));
		assertThat(rootItem.getType(), equalTo(ItemType.STORY.name()));

		StartTestItemRQ outerGivenStory = items.get(1);
		String outerGivenStoryCodeRef = GIVEN_STORIES_STORY + "/[STORY:stories/DummyScenario.story]";
		assertThat(outerGivenStory.getCodeRef(), equalTo(outerGivenStoryCodeRef));
		assertThat(outerGivenStory.getType(), equalTo(ItemType.STORY.name()));

		StartTestItemRQ rootScenario = items.get(2);
		String rootScenarioCodeRef =
				GIVEN_STORIES_STORY + "/[SCENARIO:A scenario in which the user can run additional stories as pre-requisites]";
		assertThat(rootScenario.getCodeRef(), equalTo(rootScenarioCodeRef));
		assertThat(rootScenario.getType(), equalTo(ItemType.SCENARIO.name()));

		StartTestItemRQ outerScenario = items.get(3);
		String outerScenarioCodeRef = outerGivenStoryCodeRef + "/[SCENARIO:The scenario]";
		assertThat(outerScenario.getCodeRef(), equalTo(outerScenarioCodeRef));
		assertThat(outerScenario.getType(), equalTo(ItemType.SCENARIO.name()));

		List<StartTestItemRQ> outerScenarioSteps = items.subList(4, 6);
		IntStream.range(0, outerScenarioSteps.size()).forEach(i -> {
			StartTestItemRQ rq = outerScenarioSteps.get(i);
			String outerScenarioStepCodeRef = outerScenarioCodeRef + String.format("/[STEP:%s]", DUMMY_SCENARIO_STEPS.get(i));
			assertThat(rq.getCodeRef(), equalTo(outerScenarioStepCodeRef));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
		});

		StartTestItemRQ innerStory = items.get(6);
		String innerStoryCodeRef = rootScenarioCodeRef + "/[STORY:stories/BasicInlineParameters.story]";
		assertThat(innerStory.getCodeRef(), equalTo(innerStoryCodeRef));
		assertThat(innerStory.getType(), equalTo(ItemType.STORY.name()));

		StartTestItemRQ rootStep = items.get(7);
		String rootStepCodeRef = rootScenarioCodeRef + String.format("/[STEP:%s]", GIVEN_STORY_STEP);
		assertThat(rootStep.getCodeRef(), equalTo(rootStepCodeRef));
		assertThat(rootStep.getType(), equalTo(ItemType.STEP.name()));

		StartTestItemRQ innerScenario = items.get(8);
		String innerScenarioCodeRef = innerStoryCodeRef + "/[SCENARIO:Test with a inline parameters]";
		assertThat(innerScenario.getCodeRef(), equalTo(innerScenarioCodeRef));
		assertThat(innerScenario.getType(), equalTo(ItemType.SCENARIO.name()));

		List<StartTestItemRQ> innerScenarioSteps = items.subList(9, 11);
		IntStream.range(0, innerScenarioSteps.size()).forEach(i -> {
			StartTestItemRQ rq = innerScenarioSteps.get(i);
			String innerStepCodeRef = innerScenarioCodeRef + String.format("/[STEP:%s]", INLINE_PARAMETERS_STEPS.get(i));
			assertThat(rq.getCodeRef(), equalTo(innerStepCodeRef));
			assertThat(rq.getType(), equalTo(ItemType.STEP.name()));
		});
	}
}
