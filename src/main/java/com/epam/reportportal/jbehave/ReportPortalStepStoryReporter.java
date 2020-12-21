/*
 * Copyright (C) 2019 EPAM Systems
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

import com.epam.reportportal.jbehave.util.ItemTreeUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.model.*;
import org.jbehave.core.reporters.NullStoryReporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * JBehave Reporter for reporting results into ReportPortal. Requires using
 */
public class ReportPortalStepStoryReporter extends NullStoryReporter {

	private static final String CODE_REFERENCE_DELIMITER = "/";

	private static final String CODE_REFERENCE_ITEM_TYPE_DELIMITER = ":";
	private static final String PARAMETER_ITEMS_DELIMITER = ";";
	private static final String CODE_REFERENCE_ITEM_START = "[";
	private static final String CODE_REFERENCE_ITEM_END = "]";
	private static final String START_TIME = "START_TIME";
	private static final String EXAMPLE_PATTERN = "Example: %s";
	private static final String EXAMPLE_VALUE_PATTERN = "<%s>";
	private static final Pattern EXAMPLE_VALUE_MATCH = Pattern.compile("<([^>]*)>");
	private static final String EXAMPLE = "EXAMPLE";
	private static final String EXAMPLE_PARAMETER_DELIMITER = PARAMETER_ITEMS_DELIMITER + " ";
	private static final String EXAMPLE_KEY_VALUE_DELIMITER = CODE_REFERENCE_ITEM_TYPE_DELIMITER + " ";

	private final Supplier<Launch> launch;
	private final TestItemTree itemTree;

	private final Deque<Entity<?>> structure = new LinkedList<>();

	public ReportPortalStepStoryReporter(final Supplier<Launch> launchSupplier, TestItemTree testItemTree) {
		launch = launchSupplier;
		itemTree = testItemTree;
	}

	private String getBaseCodeRef(@Nonnull final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> parents) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parents.size(); i++) {
			if (i == 0) {
				sb.append(parents.get(i).getKey().getName());
			} else {
				sb.append(CODE_REFERENCE_DELIMITER);
				Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> item = parents.get(i);
				ItemType entityType = item.getValue().getType();
				TestItemTree.ItemTreeKey key = item.getKey();
				sb.append(CODE_REFERENCE_ITEM_START)
						.append(entityType != ItemType.TEST ? entityType.name() : EXAMPLE)
						.append(CODE_REFERENCE_ITEM_TYPE_DELIMITER)
						.append(key.getName())
						.append(CODE_REFERENCE_ITEM_END);
			}
		}
		return sb.toString();
	}

	private String getCodeRef(@Nonnull final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> parents,
			@Nonnull final TestItemTree.ItemTreeKey key, ItemType type) {
		StringBuilder sb = new StringBuilder();
		if (parents.isEmpty()) {
			sb.append(key.getName());
		} else {
			sb.append(getBaseCodeRef(parents))
					.append(CODE_REFERENCE_DELIMITER)
					.append(CODE_REFERENCE_ITEM_START)
					.append(type != ItemType.TEST ? type.name() : EXAMPLE)
					.append(CODE_REFERENCE_ITEM_TYPE_DELIMITER)
					.append(key.getName())
					.append(CODE_REFERENCE_ITEM_END);
		}
		return sb.toString();
	}

	protected String getStoryName(@Nonnull final Story story) {
		return story.getName();
	}

	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final Story story) {
		Meta meta = story.getMeta();
		Set<ItemAttributesRQ> items = new HashSet<>();
		meta.getPropertyNames().forEach(n -> items.add(new ItemAttributesRQ(n, meta.getProperty(n))));
		return items;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param story     JBehave story object
	 * @param codeRef   A story code reference
	 * @param startTime a story start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartStoryRq(@Nonnull Story story, @Nonnull String codeRef, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getStoryName(story));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.STORY.name());
		rq.setAttributes(getAttributes(story));
		rq.setDescription(story.getDescription().asString());
		return rq;
	}

	protected String getScenarioName(Scenario scenario) {
		return scenario.getTitle();
	}

	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final Scenario scenario) {
		Meta meta = scenario.getMeta();
		Set<ItemAttributesRQ> items = new HashSet<>();
		meta.getPropertyNames().forEach(n -> items.add(new ItemAttributesRQ(n, meta.getProperty(n))));
		return items;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param scenario  JBehave scenario object
	 * @param codeRef   A scenario code reference
	 * @param startTime a scenario start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartScenarioRq(@Nonnull Scenario scenario, @Nonnull String codeRef, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getScenarioName(scenario));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.SCENARIO.name());
		rq.setAttributes(getAttributes(scenario));
		return rq;
	}

	/**
	 * Extension point to customize example names
	 *
	 * @param example a map of variable name -> variable value
	 * @return a name of the given example
	 */
	protected String formatExampleName(@Nonnull final Map<String, String> example) {
		return String.format(EXAMPLE_PATTERN,
				example.entrySet()
						.stream()
						.map(e -> e.getKey() + EXAMPLE_KEY_VALUE_DELIMITER + e.getValue())
						.collect(Collectors.joining(EXAMPLE_PARAMETER_DELIMITER, CODE_REFERENCE_ITEM_START, CODE_REFERENCE_ITEM_END))
		);
	}

	@Nullable
	protected List<ParameterResource> getStepParameters(@Nullable final Map<String, String> params) {
		return ofNullable(params).map(p -> p.entrySet().stream().map(e -> {
			ParameterResource param = new ParameterResource();
			param.setKey(e.getKey());
			param.setValue(e.getValue());
			return param;
		}).collect(Collectors.toList())).orElse(null);
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param example   an example map
	 * @param codeRef   a step code reference
	 * @param startTime a step start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartExampleRq(@Nonnull final Map<String, String> example, @Nonnull String codeRef,
			@Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(formatExampleName(example));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.TEST.name());
		rq.setParameters(getStepParameters(example));
		return rq;
	}

	@Nonnull
	protected List<String> getUsedParameters(@Nonnull final String step) {
		Matcher m = EXAMPLE_VALUE_MATCH.matcher(step);
		List<String> result = new ArrayList<>();
		while (m.find()) {
			result.add(m.group(1));
		}
		return result;
	}

	@Nonnull
	protected String formatExampleStep(@Nonnull final String step, @Nullable final Map<String, String> example) {
		if (example == null) {
			return step;
		}
		String result = step;
		for (Map.Entry<String, String> e : example.entrySet()) {
			result = result.replaceAll(String.format(EXAMPLE_VALUE_PATTERN, e.getKey()), e.getValue());
		}
		return result;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param step      JBehave step name
	 * @param codeRef   a step code reference
	 * @param startTime a step start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartStepRq(@Nonnull final String step, @Nonnull final String codeRef,
			@Nullable final Map<String, String> params, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(formatExampleStep(step, params));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.STEP.name());
		ofNullable(params).ifPresent(p -> rq.setParameters(getStepParameters(getUsedParameters(step).stream()
				.collect(Collectors.toMap(s -> s, p::get)))));
		return rq;
	}

	protected TestItemTree.TestItemLeaf createLeaf(@Nonnull final ItemType type, @Nonnull final StartTestItemRQ rq,
			@Nullable final Maybe<String> parentId) {
		Launch myLaunch = launch.get();
		TestItemTree.TestItemLeaf l = ofNullable(parentId).map(p -> TestItemTree.createTestItemLeaf(p, myLaunch.startTestItem(p, rq)))
				.orElseGet(() -> TestItemTree.createTestItemLeaf(myLaunch.startTestItem(rq)));
		l.setType(type);
		l.setAttribute(START_TIME, rq.getStartTime());
		return l;
	}

	@SuppressWarnings("unchecked")
	protected TestItemTree.TestItemLeaf retrieveLeaf() {
		final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> leafChain = new ArrayList<>();
		Entity<?> previousEntity = null;
		for (Entity<?> entity : structure) {
			final ItemType itemType = entity.type();
			final Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> parent = leafChain.isEmpty() ?
					null :
					leafChain.get(leafChain.size() - 1);
			final Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = ofNullable(parent).map(p -> p.getValue()
					.getChildItems()).orElseGet(itemTree::getTestItems);
			final Maybe<String> parentId = ofNullable(parent).map(p -> p.getValue().getItemId()).orElse(null);
			final Date previousDate = ofNullable(parent).map(p -> p.getValue().<Date>getAttribute(START_TIME))
					.orElseGet(() -> Calendar.getInstance().getTime());
			Date currentDate = Calendar.getInstance().getTime();
			Date itemDate;
			if (previousDate.compareTo(currentDate) <= 0) {
				itemDate = currentDate;
			} else {
				itemDate = previousDate;
			}
			switch (itemType) {
				case STORY:
					Story story = (Story) entity.get();
					TestItemTree.ItemTreeKey storyKey = ItemTreeUtils.createKey(story);
					leafChain.add(ImmutablePair.of(storyKey, children.computeIfAbsent(storyKey, k -> createLeaf(ItemType.STORY,
							buildStartStoryRq(story, getCodeRef(leafChain, k, ItemType.STORY), itemDate),
							parentId
					))));
					break;
				case SCENARIO:
					Scenario scenario = (Scenario) entity.get();
					TestItemTree.ItemTreeKey scenarioKey = ItemTreeUtils.createKey(scenario);
					leafChain.add(ImmutablePair.of(scenarioKey, children.computeIfAbsent(scenarioKey, k -> createLeaf(ItemType.SCENARIO,
							buildStartScenarioRq(scenario, getCodeRef(leafChain, k, ItemType.SCENARIO), itemDate),
							parentId
					))));
					break;
				case TEST: // type TEST == an Example
					Map<String, String> example = (Map<String, String>) entity.get();
					TestItemTree.ItemTreeKey exampleKey = ItemTreeUtils.createKey(example);
					leafChain.add(ImmutablePair.of(exampleKey, children.computeIfAbsent(exampleKey, k -> createLeaf(ItemType.TEST,
							buildStartExampleRq(example, getCodeRef(leafChain, k, ItemType.TEST), itemDate),
							parentId
					))));
					break;
				case STEP:
					boolean hasExample = ofNullable(previousEntity).map(e -> e.type).orElse(null) == ItemType.TEST;
					Map<String, String> stepParams = hasExample ? (Map<String, String>) previousEntity.get() : null;
					String stepName = (String) entity.get();
					TestItemTree.ItemTreeKey stepKey = ItemTreeUtils.createKey(stepName);
					leafChain.add(ImmutablePair.of(stepKey, children.computeIfAbsent(stepKey, k -> createLeaf(ItemType.STEP,
							buildStartStepRq(stepName, getCodeRef(leafChain, k, ItemType.STEP), stepParams, itemDate),
							parentId
					))));
					break;
			}
			previousEntity = entity;
		}
		return leafChain.get(leafChain.size() - 1).getValue();
	}

	@SuppressWarnings("unchecked")
	protected TestItemTree.TestItemLeaf getLeaf() {
		final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> leafChain = new ArrayList<>();
		for (Entity<?> entity : structure) {
			final ItemType itemType = entity.type();
			final Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> parent = leafChain.isEmpty() ?
					null :
					leafChain.get(leafChain.size() - 1);
			if (parent != null) {
				if (parent.getValue() == null) {
					return null;
				}
			}
			final Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = ofNullable(parent).map(p -> p.getValue()
					.getChildItems()).orElseGet(itemTree::getTestItems);
			switch (itemType) {
				case STORY:
					TestItemTree.ItemTreeKey storyKey = ItemTreeUtils.createKey((Story) entity.get());
					leafChain.add(ImmutablePair.of(storyKey, children.get(storyKey)));
					break;
				case SCENARIO:
					TestItemTree.ItemTreeKey scenarioKey = ItemTreeUtils.createKey((Scenario) entity.get());
					leafChain.add(ImmutablePair.of(scenarioKey, children.get(scenarioKey)));
					break;
				case TEST: // type TEST == an Example
					TestItemTree.ItemTreeKey exampleKey = ItemTreeUtils.createKey((Map<String, String>) entity.get());
					leafChain.add(ImmutablePair.of(exampleKey, children.get(exampleKey)));
					break;
				case STEP:
					TestItemTree.ItemTreeKey stepKey = ItemTreeUtils.createKey((String) entity.get());
					leafChain.add(ImmutablePair.of(stepKey, children.get(stepKey)));
					break;
			}
		}
		return leafChain.get(leafChain.size() - 1).getValue();
	}

	/**
	 * Finishes the last item in the structure
	 */
	protected void finishLastItem(@Nullable final ItemStatus status) {
		TestItemTree.TestItemLeaf item = getLeaf();
		ofNullable(item).ifPresent(i -> {
			FinishTestItemRQ rq = new FinishTestItemRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			rq.setStatus(ofNullable(status).map(Enum::name).orElse(null));
			launch.get().finishTestItem(i.getItemId(), rq);
		});
		structure.pollLast();
	}

	@Override
	public void storyCancelled(Story story, StoryDuration storyDuration) {
		finishLastItem(ItemStatus.SKIPPED);
	}

	/**
	 * Starts story (test suite level) in ReportPortal
	 *
	 * @param story      JBehave story object
	 * @param givenStory whether or not this story is given
	 * @see <a href="https://jbehave.org/reference/latest/given-stories.html">Given Stories</a>
	 */
	@Override
	public void beforeStory(Story story, boolean givenStory) {
		structure.add(new Entity<>(ItemType.STORY, story));
	}

	/**
	 * Finishes story in ReportPortal
	 */
	@Override
	public void afterStory(boolean givenStory) {
		finishLastItem(null);
	}

	/**
	 * Starts scenario in ReportPortal (test level)
	 *
	 * @param scenario JBehave scenario object
	 */
	@Override
	public void beforeScenario(Scenario scenario) {
		structure.add(new Entity<>(ItemType.SCENARIO, scenario));
	}

	/**
	 * Finishes scenario in ReportPortal
	 */
	@Override
	public void afterScenario() {
		finishLastItem(null);
	}

	/**
	 * Starts step in ReportPortal (TestStep level)
	 *
	 * @param step Step to be reported
	 */
	@Override
	public void beforeStep(String step) {
		structure.add(new Entity<>(ItemType.STEP, step));
		retrieveLeaf();
	}

	@Override
	public void example(Map<String, String> tableRow, int exampleIndex) {
		Entity<?> e = structure.getLast();
		if (e.type() == ItemType.TEST) {
			finishLastItem(null);
		}
		structure.add(new Entity<>(ItemType.TEST, tableRow)); // type TEST is used for Examples
	}

	@Override
	public void beforeExamples(List<String> steps, ExamplesTable table) {
		// do nothing we will handle it on 'example' method call
	}

	@Override
	public void afterExamples() {
		finishLastItem(null);
	}

	protected void finishStep(TestItemTree.TestItemLeaf step, ItemStatus status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status.name());
		launch.get().finishTestItem(step.getItemId(), rq);
	}

	/**
	 * Finishes step in ReportPortal
	 */
	@Override
	public void successful(String step) {
		finishLastItem(ItemStatus.PASSED);
	}

	@Override
	public void ignorable(String step) {
		structure.add(new Entity<>(ItemType.STEP, step));
		TestItemTree.TestItemLeaf item = retrieveLeaf();
		structure.pollLast();
		finishStep(item, ItemStatus.SKIPPED);
	}

	@Override
	public void notPerformed(String step) {
		structure.add(new Entity<>(ItemType.STEP, step));
		TestItemTree.TestItemLeaf item = retrieveLeaf();
		structure.pollLast();
		finishStep(item, ItemStatus.SKIPPED);
	}

	@Override
	public void failed(String step, Throwable cause) {
		JBehaveUtils.sendStackTraceToRP(cause);
		TestItemTree.TestItemLeaf item = retrieveLeaf();
		structure.pollLast();
		finishStep(item, ItemStatus.FAILED);
	}

	private void simulateStep(String step) {
		structure.add(new Entity<>(ItemType.STEP, step));
		TestItemTree.TestItemLeaf item = retrieveLeaf();
		structure.pollLast();
		finishStep(item, ItemStatus.SKIPPED);
	}

	@Override
	public void pending(String step) {
		simulateStep(step);
	}

	@Override
	public void scenarioNotAllowed(Scenario scenario, String filter) {
		if (null != scenario.getExamplesTable() && scenario.getExamplesTable().getRowCount() > 0) {
			beforeExamples(scenario.getSteps(), scenario.getExamplesTable());
			for (int i = 0; i < scenario.getExamplesTable().getRowCount(); i++) {
				example(scenario.getExamplesTable().getRow(i));
				for (String step : scenario.getSteps()) {
					simulateStep(step);
				}
			}
		} else {
			for (String step : scenario.getSteps()) {
				simulateStep(step);
			}
		}
		finishLastItem(ItemStatus.SKIPPED);
	}

	protected static class Entity<T> {

		private final ItemType type;
		private final T value;

		public Entity(ItemType itemType, T itemValue) {
			type = itemType;
			value = itemValue;
		}

		public ItemType type() {
			return type;
		}

		public T get() {
			return value;
		}

	}

}
