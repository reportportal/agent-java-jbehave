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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.Constants;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.core.type.TypeReference;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import okio.Buffer;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.embedder.NullEmbedderMonitor;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.UnderscoredCamelCaseResolver;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.parsers.StoryParser;
import org.jbehave.core.reporters.FilePrintStreamFactory;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.epam.reportportal.util.test.CommonUtils.createMaybe;
import static com.epam.reportportal.util.test.CommonUtils.generateUniqueId;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * This class shouldn't be like that, since it goes against inheritance philosophy. But this is my try to workaround Mockito context
 * collision issues on virtual environments. Basically, if I made everything right, I will get test failures sporadically.
 */
public class BaseTest {
	public static final String ROOT_SUITE_PREFIX = "root_";

	public static ExecutorService testExecutor() {
		return Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
	}

	public static void run(@Nonnull final Class<?> clazz, @Nonnull final Format format, @Nonnull final List<String> stories,
			@Nonnull final StoryParser parser, @Nullable final Object... steps) {
		Properties viewResources = new Properties();

		Embedder embedder = new Embedder();
		embedder.useEmbedderMonitor(new NullEmbedderMonitor());
		embedder.useEmbedderControls(new EmbedderControls().doGenerateViewAfterStories(false)
				.doFailOnStoryTimeout(false)
				.doBatch(false)
				.doIgnoreFailureInStories(true)
				.doIgnoreFailureInView(true)
				.doVerboseFailures(false)
				.doVerboseFiltering(false));

		embedder.useConfiguration(new MostUsefulConfiguration().useStoryLoader(new LoadFromClasspath(clazz))
				.useStoryPathResolver(new UnderscoredCamelCaseResolver())
				.useStoryParser(parser)
				.useStoryReporterBuilder(new StoryReporterBuilder().withDefaultFormats()
						.withPathResolver(new FilePrintStreamFactory.ResolveToPackagedName())
						.withViewResources(viewResources)
						.withFormats(format)
						.withFailureTrace(true)
						.withFailureTraceCompression(true)));

		final InjectableStepsFactory stepsFactory = new InstanceStepsFactory(embedder.configuration(),
				steps == null ? Collections.emptyList() : Arrays.asList(steps)
		);
		embedder.useCandidateSteps(stepsFactory.createCandidateSteps());
		embedder.runStoriesAsPaths(stories);
	}

	public void run(@Nonnull final Format format, @Nonnull final List<String> stories, @Nonnull final StoryParser parser,
			@Nullable final Object... steps) {
		run(getClass(), format, stories, parser, steps);
	}

	public void run(@Nonnull final Format format, @Nonnull final List<String> stories, @Nullable final Object... steps) {
		run(format, stories, new RegexStoryParser(), steps);
	}

	public void run(@Nonnull final Format format, @Nonnull final String story, @Nullable final Object... steps) {
		run(format, Collections.singletonList(story), steps);
	}

	public static void mockLaunch(@Nonnull final ReportPortalClient client, @Nullable final String launchUuid,
			@Nullable final String storyUuid, @Nonnull String testClassUuid, @Nonnull String stepUuid) {
		mockLaunch(client, launchUuid, storyUuid, testClassUuid, Collections.singleton(stepUuid));
	}

	public static void mockLaunch(@Nonnull final ReportPortalClient client, @Nullable final String launchUuid,
			@Nullable final String storyUuid, @Nonnull String testClassUuid, @Nonnull Collection<String> stepList) {
		mockLaunch(client, launchUuid, storyUuid, Collections.singletonList(Pair.of(testClassUuid, stepList)));
	}

	public static <T extends Collection<String>> void mockLaunch(@Nonnull final ReportPortalClient client,
			@Nullable final String launchUuid, @Nullable final String storyUuid, @Nonnull final Collection<Pair<String, T>> testSteps) {
		String launch = ofNullable(launchUuid).orElse(CommonUtils.namedId("launch_"));
		when(client.startLaunch(any())).thenReturn(createMaybe(new StartLaunchRS(launch, 1L)));
		when(client.finishLaunch(eq(launch), any())).thenReturn(createMaybe(new OperationCompletionRS()));

		mockStory(client, storyUuid, testSteps);
	}

	public static <T extends Collection<String>> void mockStory(@Nonnull final ReportPortalClient client, @Nullable final String storyUuid,
			@Nonnull final Collection<Pair<String, T>> testSteps) {
		String rootItemId = ofNullable(storyUuid).orElseGet(() -> CommonUtils.namedId(ROOT_SUITE_PREFIX));
		mockStories(client, Collections.singletonList(Pair.of(rootItemId, testSteps)));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Collection<String>> void mockStories(@Nonnull final ReportPortalClient client,
			@Nonnull final List<Pair<String, Collection<Pair<String, T>>>> stories) {
		if (stories.isEmpty()) {
			return;
		}
		String firstStory = stories.get(0).getKey();
		Maybe<ItemCreatedRS> first = createMaybe(new ItemCreatedRS(firstStory, firstStory));
		Maybe<ItemCreatedRS>[] other = (Maybe<ItemCreatedRS>[]) stories.subList(1, stories.size())
				.stream()
				.map(Pair::getKey)
				.map(s -> createMaybe(new ItemCreatedRS(s, s)))
				.toArray(Maybe[]::new);
		when(client.startTestItem(any())).thenReturn(first, other);

		stories.forEach(i -> {
			Maybe<OperationCompletionRS> rootFinishMaybe = createMaybe(new OperationCompletionRS());
			when(client.finishTestItem(same(i.getKey()), any())).thenReturn(rootFinishMaybe);
			mockScenario(client, i.getKey(), i.getValue());
		});
	}

	@SuppressWarnings("unchecked")
	public static <T extends Collection<String>> void mockScenario(@Nonnull final ReportPortalClient client,
			@Nonnull final String storyUuid, @Nonnull final Collection<Pair<String, T>> testSteps) {
		List<Maybe<ItemCreatedRS>> testResponses = testSteps.stream()
				.map(Pair::getKey)
				.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
				.collect(Collectors.toList());

		Maybe<ItemCreatedRS> first = testResponses.get(0);
		Maybe<ItemCreatedRS>[] other = testResponses.subList(1, testResponses.size()).toArray(new Maybe[0]);
		when(client.startTestItem(same(storyUuid), any())).thenReturn(first, other);

		testSteps.forEach(test -> {
			String testClassUuid = test.getKey();
			List<Maybe<ItemCreatedRS>> stepResponses = test.getValue()
					.stream()
					.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());
			when(client.finishTestItem(same(testClassUuid), any())).thenReturn(createMaybe(new OperationCompletionRS()));
			if (!stepResponses.isEmpty()) {
				Maybe<ItemCreatedRS> myFirst = stepResponses.get(0);
				Maybe<ItemCreatedRS>[] myOther = stepResponses.subList(1, stepResponses.size()).toArray(new Maybe[0]);
				when(client.startTestItem(same(testClassUuid), any())).thenReturn(myFirst, myOther);
				new HashSet<>(test.getValue()).forEach(testMethodUuid -> when(client.finishTestItem(same(testMethodUuid),
						any()
				)).thenReturn(createMaybe(new OperationCompletionRS())));
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static void mockBatchLogging(final ReportPortalClient client) {
		when(client.log(any(List.class))).thenReturn(createMaybe(new BatchSaveOperatingRS()));
	}

	public static void mockNestedSteps(final ReportPortalClient client, final Pair<String, String> parentNestedPair) {
		mockNestedSteps(client, Collections.singletonList(parentNestedPair));
	}

	@SuppressWarnings("unchecked")
	public static void mockNestedSteps(final ReportPortalClient client, final List<Pair<String, String>> parentNestedPairs) {
		Map<String, List<String>> responseOrders = parentNestedPairs.stream()
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		responseOrders.forEach((k, v) -> {
			List<Maybe<ItemCreatedRS>> responses = v.stream()
					.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());

			Maybe<ItemCreatedRS> first = responses.get(0);
			Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
			when(client.startTestItem(eq(k), any())).thenReturn(first, other);
		});
		parentNestedPairs.forEach(p -> when(client.finishTestItem(same(p.getValue()),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> createMaybe(new OperationCompletionRS())));
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setLaunchName("My-test-launch" + generateUniqueId());
		result.setProjectName("test-project");
		result.setEnable(true);
		result.setCallbackReportingEnabled(true);
		result.setBaseUrl("http://localhost:8080");
		return result;
	}

	public static List<SaveLogRQ> extractJsonParts(List<MultipartBody.Part> parts) {
		return parts.stream()
				.filter(p -> ofNullable(p.headers()).map(headers -> headers.get("Content-Disposition"))
						.map(h -> h.contains(Constants.LOG_REQUEST_JSON_PART))
						.orElse(false))
				.map(MultipartBody.Part::body)
				.map(b -> {
					Buffer buf = new Buffer();
					try {
						b.writeTo(buf);
					} catch (IOException ignore) {
					}
					return buf.readByteArray();
				})
				.map(b -> {
					try {
						return HttpRequestUtils.MAPPER.readValue(b, new TypeReference<List<SaveLogRQ>>() {
						});
					} catch (IOException e) {
						return Collections.<SaveLogRQ>emptyList();
					}
				})
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public static List<SaveLogRQ> filterLogs(ArgumentCaptor<List<MultipartBody.Part>> logCaptor, Predicate<SaveLogRQ> filter) {
		return logCaptor.getAllValues().stream().flatMap(l -> extractJsonParts(l).stream()).filter(filter).collect(Collectors.toList());
	}

	public static void verifyLogged(@Nonnull final ArgumentCaptor<List<MultipartBody.Part>> logCaptor, @Nullable final String itemId,
			@Nonnull final LogLevel level, @Nonnull final String message) {
		List<SaveLogRQ> expectedErrorList = filterLogs(logCaptor,
				l -> level.name().equals(l.getLevel()) && l.getMessage() != null && l.getMessage().contains(message)
		);
		assertThat(expectedErrorList, hasSize(1));
		SaveLogRQ expectedError = expectedErrorList.get(0);
		assertThat(expectedError.getItemUuid(), equalTo(itemId));
	}
}
