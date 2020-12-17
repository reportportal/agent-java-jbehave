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
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.model.*;
import org.jbehave.core.reporters.NullStoryReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * JBehave Reporter for reporting results into ReportPortal. Requires using
 */
public class ReportPortalStepStoryReporter extends NullStoryReporter {

	private static final String CODE_REFERENCE_DELIMITER = "/";

	private static final String CODE_REFERENCE_ITEM_TYPE_DELIMITER = ":";
	private static final String CODE_REFERENCE_ITEM_START = "[";
	private static final String CODE_REFERENCE_ITEM_END = "]";
	private static final String START_TIME = "START_TIME";

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
						.append(entityType.name())
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
					.append(type.name())
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
	 * Extension point to customize test creation event/request
	 *
	 * @param step      JBehave step name
	 * @param codeRef   a step code reference
	 * @param startTime a step start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartStepRq(@Nonnull String step, @Nonnull String codeRef, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(step);
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.STEP.name());
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

	protected TestItemTree.TestItemLeaf retrieveLeaf() {
		final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> leafChain = new ArrayList<>();
		for (Entity<?> e : structure) {
			final ItemType itemType = e.type();
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
					Story story = (Story) e.get();
					TestItemTree.ItemTreeKey storyKey = ItemTreeUtils.createKey(story);
					leafChain.add(ImmutablePair.of(storyKey, children.computeIfAbsent(storyKey, k -> createLeaf(ItemType.STORY,
							buildStartStoryRq(story, getCodeRef(leafChain, k, ItemType.STORY), itemDate),
							parentId
					))));
					break;
				case SCENARIO:
					Scenario scenario = (Scenario) e.get();
					TestItemTree.ItemTreeKey scenarioKey = ItemTreeUtils.createKey(scenario);
					leafChain.add(ImmutablePair.of(scenarioKey, children.computeIfAbsent(scenarioKey, k -> createLeaf(ItemType.SCENARIO,
							buildStartScenarioRq(scenario, getCodeRef(leafChain, k, ItemType.SCENARIO), itemDate),
							parentId
					))));
					break;
				case STEP:
					String step = (String) e.get();
					TestItemTree.ItemTreeKey stepKey = ItemTreeUtils.createKey(step);
					leafChain.add(ImmutablePair.of(stepKey,
							children.computeIfAbsent(
									stepKey,
									k -> createLeaf(ItemType.STEP,
											buildStartStepRq(step, getCodeRef(leafChain, k, ItemType.STEP), itemDate),
											parentId
									)
							)
					));
					break;
			}
		}
		return leafChain.get(leafChain.size() - 1).getValue();
	}

	protected TestItemTree.TestItemLeaf getLeaf() {
		final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> leafChain = new ArrayList<>();
		for (Entity<?> e : structure) {
			final ItemType itemType = e.type();
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
					TestItemTree.ItemTreeKey storyKey = ItemTreeUtils.createKey((Story) e.get());
					leafChain.add(ImmutablePair.of(storyKey, children.get(storyKey)));
					break;
				case SCENARIO:
					TestItemTree.ItemTreeKey scenarioKey = ItemTreeUtils.createKey((Scenario) e.get());
					leafChain.add(ImmutablePair.of(scenarioKey, children.get(scenarioKey)));
					break;
				case STEP:
					TestItemTree.ItemTreeKey stepKey = ItemTreeUtils.createKey((String) e.get());
					leafChain.add(ImmutablePair.of(stepKey, children.get(stepKey)));
					break;
			}
		}
		return leafChain.get(leafChain.size() - 1).getValue();
	}

	/**
	 * Finishes the last item in the structure
	 */
	protected void finishLastItem(ItemStatus status) {
		TestItemTree.TestItemLeaf item = getLeaf();
		ofNullable(item).ifPresent(i -> {
			FinishTestItemRQ rq = new FinishTestItemRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			rq.setStatus(status.name());
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
	 * @param story
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
		finishLastItem(ItemStatus.PASSED);
	}

	/**
	 * Starts scenario in ReportPortal (test level)
	 *
	 * @param scenario
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
		finishLastItem(ItemStatus.PASSED);
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
		JBehaveContext.getCurrentStory().getExamples().setCurrentExample(exampleIndex);
	}

	@Override
	public void beforeExamples(List<String> steps, ExamplesTable table) {
		JBehaveContext.getCurrentStory().setExamples(new JBehaveContext.Examples(steps, table));
	}

	@Override
	public void afterExamples() {
		JBehaveContext.getCurrentStory().setExamples(null);
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
