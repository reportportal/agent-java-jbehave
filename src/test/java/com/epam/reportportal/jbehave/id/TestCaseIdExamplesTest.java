package com.epam.reportportal.jbehave.id;

import com.epam.reportportal.jbehave.BaseTest;
import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.integration.basic.StockSteps;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class TestCaseIdExamplesTest extends BaseTest {

	private final String storyId = CommonUtils.namedId("story_");
	private final String scenarioId = CommonUtils.namedId("scenario_");
	private final List<String> exampleIds = Stream.generate(() -> CommonUtils.namedId("example_")).limit(2).collect(Collectors.toList());

	private final List<Pair<String, String>> stepIds = exampleIds.stream()
			.flatMap(e -> Stream.generate(() -> Pair.of(e, CommonUtils.namedId("step_"))).limit(3))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ReportPortalStepFormat format = new ReportPortalStepFormat(ReportPortal.create(client,
			standardParameters(),
			Executors.newSingleThreadExecutor()
	));

	@BeforeEach
	public void setupMock() {
		mockLaunch(client, null, storyId, scenarioId, exampleIds);
		mockNestedSteps(client, stepIds);
		mockBatchLogging(client);

	}

	private static final List<String> EXAMPLE_NODES = Stream.concat(Collections.nCopies(3,
			"[EXAMPLE:[symbol:STK1;threshold:10.0;price:5.0;status:OFF]]"
			).stream(),
			Collections.nCopies(3, "[EXAMPLE:[symbol:STK1;threshold:10.0;price:11.0;status:ON]]").stream()
	).collect(Collectors.toList());

	private static final List<String> STEP_NAMES = Arrays.asList("Given a stock of symbol <symbol> and a threshold <threshold>",
			"When the stock is traded at price <price>",
			"Then the alert status should be status <status>",
			"Given a stock of symbol <symbol> and a threshold <threshold>",
			"When the stock is traded at price <price>",
			"Then the alert status should be status <status>"
	);

	private static final List<Map<String, String>> STEP_PARAMETERS = Arrays.asList(new HashMap<String, String>() {{
		put("symbol", "STK1");
		put("threshold", "10.0");
	}}, new HashMap<String, String>() {{
		put("price", "5.0");
	}}, new HashMap<String, String>() {{
		put("status", "OFF");
	}}, new HashMap<String, String>() {{
		put("symbol", "STK1");
		put("threshold", "10.0");
	}}, new HashMap<String, String>() {{
		put("price", "11.0");
	}}, new HashMap<String, String>() {{
		put("status", "ON");
	}});

	private static final String EXAMPLES_STORY = "stories/Examples.story";

	@Test
	public void verify_test_case_id_with_examples() {
		run(format, EXAMPLES_STORY, new StockSteps());

		verify(client, times(1)).startTestItem(any());
		verify(client, times(1)).startTestItem(same(storyId), any());
		verify(client, times(2)).startTestItem(same(scenarioId), any());
		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(3)).startTestItem(same(exampleIds.get(0)), startCaptor.capture());
		verify(client, times(3)).startTestItem(same(exampleIds.get(1)), startCaptor.capture());

		String scenarioCodeRef = EXAMPLES_STORY + "/[SCENARIO:Stock trade alert]";

		// Start items verification
		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		List<StartTestItemRQ> steps = startItems.subList(0, 6);
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ rq = steps.get(i);
			String exampleCodeRef = scenarioCodeRef + "/" + EXAMPLE_NODES.get(i);
			String stepCodeRef = exampleCodeRef + "/" + String.format("[STEP:%s]", STEP_NAMES.get(i));
			assertThat(rq.getTestCaseId(),
					equalTo(stepCodeRef + STEP_PARAMETERS.get(i).values().stream().collect(Collectors.joining(",", "[", "]")))
			);
		});
	}
}
