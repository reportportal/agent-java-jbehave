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

import com.epam.reportportal.jbehave.util.ItemTreeUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.StatusEvaluation;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.reportportal.utils.markdown.MarkdownUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.model.*;
import org.jbehave.core.reporters.NullStoryReporter;
import org.jbehave.core.steps.StepCreator;
import org.jbehave.core.steps.Timing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * JBehave Reporter for reporting results into ReportPortal.
 *
 * @author Vadzim Hushchanskou
 */
public abstract class ReportPortalStoryReporter extends NullStoryReporter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalStoryReporter.class);

	public static final String CODE_REF = "CODE_REF";
	public static final String START_TIME = "START_TIME";
	public static final String PARAMETERS = "PARAMETERS";
	public static final String PARENT = "PARENT";
	public static final String START_REQUEST = "START_REQUEST";
	public static final String FINISH_REQUEST = "FINISH_REQUEST";

	private static final String CODE_REFERENCE_DELIMITER = "/";
	private static final String CODE_REFERENCE_ITEM_TYPE_DELIMITER = ":";
	private static final String CODE_REFERENCE_ITEM_START = "[";
	private static final String CODE_REFERENCE_ITEM_END = "]";
	private static final String EXAMPLE_VALUE_PATTERN = "<%s>";
	private static final Pattern EXAMPLE_VALUE_MATCH = Pattern.compile("<([^>]*)>");
	private static final String EXAMPLE = "EXAMPLE";
	private static final String LIFECYCLE = "LIFECYCLE";
	private static final String NO_NAME = "No name";
	private static final String BEFORE_STORIES = "BeforeStories";
	private static final String AFTER_STORIES = "AfterStories";
	private static final String BEFORE_STORY = "BeforeStory";
	private static final String AFTER_STORY = "AfterStory";
	private static final String PARAMETERS_PATTERN = "Parameters:\n\n%s";

	private final Deque<Entity<?>> structure = new LinkedList<>();
	private final Deque<TestItemTree.TestItemLeaf> stepStack = new LinkedList<>();
	private final Supplier<Launch> launch;
	private final TestItemTree itemTree;

	private TestItemTree.TestItemLeaf lastStep;
	private static volatile ItemType currentLifecycleTopItemType;
	private ItemType currentLifecycleItemType;

	public ReportPortalStoryReporter(final Supplier<Launch> launchSupplier, TestItemTree testItemTree) {
		launch = launchSupplier;
		itemTree = testItemTree;
	}

	/**
	 * Returns an item leaf of the last step reported with a reporter instance
	 *
	 * @return an item leaf or empty if no steps were reported
	 */
	@Nonnull
	public Optional<TestItemTree.TestItemLeaf> getLastStep() {
		return ofNullable(lastStep);
	}

	/**
	 * Generates code references (path through a story to a step) for JBehave stories.
	 *
	 * @param parentCodeRef a basis code reference or null if it's a root item
	 * @param key           an item leaf key
	 * @param type          an item type
	 * @return a code reference string to identify every element of a story
	 */
	private String getCodeRef(@Nullable final String parentCodeRef, @Nonnull final TestItemTree.ItemTreeKey key,
			@Nonnull ItemType type) {
		StringBuilder sb = new StringBuilder();
		if (isBlank(parentCodeRef)) {
			sb.append(key.getName());
		} else {
			String typeName;
			switch (type) {
				case SUITE:
					typeName = EXAMPLE;
					break;
				case TEST:
					typeName = LIFECYCLE;
					break;
				default:
					typeName = type.name();
			}
			sb.append(parentCodeRef)
					.append(CODE_REFERENCE_DELIMITER)
					.append(CODE_REFERENCE_ITEM_START)
					.append(typeName)
					.append(CODE_REFERENCE_ITEM_TYPE_DELIMITER)
					.append(key.getName().replace("\n", "").replace("\r", ""))
					.append(CODE_REFERENCE_ITEM_END);
		}
		return sb.toString();
	}

	/**
	 * Extension point to customize story naming. Returns a story name.
	 *
	 * @param story JBehave's story object
	 * @return the story name
	 */
	protected String getStoryName(@Nonnull final Story story) {
		return story.getName();
	}

	/**
	 * Converts a JBehave {@link Meta} object into a {@link Set} of {@link ItemAttributesRQ} ready to use in a request to Report Portal
	 *
	 * @param meta JBehave's meta object
	 * @return a set of attributes
	 */
	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final Meta meta) {
		Set<ItemAttributesRQ> items = new HashSet<>();
		meta.getPropertyNames().forEach(name -> {
			String value = meta.getProperty(name);
			items.add(isBlank(value) ? new ItemAttributesRQ(name) : new ItemAttributesRQ(name, value));
		});
		return items;
	}

	/**
	 * Retrieves story metas and converts it into a {@link Set} of {@link ItemAttributesRQ} ready to use in a request to Report Portal
	 *
	 * @param story JBehave's story object
	 * @return a set of attributes
	 */
	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final Story story) {
		return getAttributes(story.getMeta());
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
	protected StartTestItemRQ buildStartStoryRq(@Nonnull Story story, @Nonnull String codeRef,
			@Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getStoryName(story));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.STORY.name());
		rq.setAttributes(getAttributes(story));
		rq.setDescription(story.getDescription().asString());
		return rq;
	}

	/**
	 * Extension point to customize scenario naming. Returns a scenario name.
	 *
	 * @param scenario JBehave's scenario object
	 * @return the scenario name
	 */
	protected String getScenarioName(Scenario scenario) {
		String title = scenario.getTitle();
		return isBlank(title) ? NO_NAME : title;
	}

	/**
	 * Retrieves scenario metas and converts it into a {@link Set} of {@link ItemAttributesRQ} ready to use in a request to Report Portal
	 *
	 * @param scenario JBehave's scenario object
	 * @return a set of attributes
	 */
	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final Scenario scenario) {
		return getAttributes(scenario.getMeta());
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
	protected StartTestItemRQ buildStartScenarioRq(@Nonnull Scenario scenario, @Nonnull String codeRef,
			@Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getScenarioName(scenario));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.SCENARIO.name());
		rq.setAttributes(getAttributes(scenario));
		return rq;
	}

	/**
	 * Converts parameters map into Report Portal's {@link ParameterResource} list ready to use in a request to Report Portal
	 *
	 * @param params a parameters map
	 * @return a list of parameters
	 */
	@Nullable
	protected List<ParameterResource> getStepParameters(@Nullable final Map<String, String> params) {
		return ofNullable(params).map(p -> p.entrySet()
				.stream()
				.map(e -> parameterOf(e.getKey(), e.getValue()))
				.collect(Collectors.toList())).orElse(null);
	}

	private ParameterResource parameterOf(String key, String value) {
		ParameterResource param = new ParameterResource();
		param.setKey(key);
		param.setValue(value);
		return param;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param scenario  JBehave scenario object
	 * @param example   an example map
	 * @param codeRef   a step code reference
	 * @param startTime a step start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartExampleRq(@Nonnull Scenario scenario, @Nonnull Map<String, String> example, @Nonnull String codeRef,
			@Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getScenarioName(scenario));
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.TEST.name());
		rq.setParameters(getStepParameters(example));
		rq.setDescription(String.format(PARAMETERS_PATTERN, MarkdownUtils.formatDataTable(example)));
		return rq;
	}

	/**
	 * Return parameter names used in bypassed step
	 *
	 * @param step a step name pattern
	 * @return a list of parameter names from the step
	 */
	@Nonnull
	protected List<String> getUsedParameters(@Nonnull final String step) {
		Matcher m = EXAMPLE_VALUE_MATCH.matcher(step);
		List<String> result = new ArrayList<>();
		while (m.find()) {
			result.add(m.group(1));
		}
		return result;
	}

	/**
	 * Formats an Example test name
	 *
	 * @param step    a step name pattern
	 * @param example example parameters map
	 * @return step name
	 */
	@Nonnull
	protected String formatExampleStep(@Nonnull final String step, @Nullable final Map<String, String> example) {
		if (example == null) {
			return step;
		}
		String result = step;
		for (Map.Entry<String, String> e : example.entrySet()) {
			result = result.replaceAll(String.format(EXAMPLE_VALUE_PATTERN, e.getKey()),
					Matcher.quoteReplacement(e.getValue())
			);
		}
		return result;
	}

	/**
	 * Creates a {@link TestCaseIdEntry} by code reference and parameter map.
	 *
	 * @param codeRef a test code reference
	 * @param params  test parameters map (if any)
	 * @return Test Case ID or null if no coderef nor params were bypassed
	 */
	@Nullable
	protected TestCaseIdEntry getTestCaseId(@Nullable String codeRef, @Nullable final List<String> params) {
		return TestCaseIdUtils.getTestCaseId(codeRef, params);
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param step      JBehave step name
	 * @param codeRef   a step code reference
	 * @param params    Examples parameters for current iteration
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
		Optional<List<ParameterResource>> usedParams = ofNullable(params).map(p -> getUsedParameters(step).stream()
				.filter(params::containsKey)
				.map(pk -> parameterOf(pk, params.get(pk)))
				.collect(Collectors.toList()));
		usedParams.ifPresent(rq::setParameters);
		rq.setTestCaseId(ofNullable(getTestCaseId(codeRef,
				usedParams.map(p -> p.stream().map(ParameterResource::getValue).collect(Collectors.toList()))
						.orElse(null)
		)).map(TestCaseIdEntry::getId).orElse(null));
		return rq;
	}

	/**
	 * Extension point to customize lifecycle suite (before/after) creation event/request
	 *
	 * @param name      a lifecycle suite name
	 * @param codeRef   the suite code reference
	 * @param startTime item start time
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildLifecycleSuiteStartRq(@Nonnull final String name, @Nullable String codeRef,
			@Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(name);
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(ItemType.TEST.name());
		return rq;
	}

	/**
	 * Extension point to customize lifecycle method (before/after) creation event/request
	 *
	 * @param type      item type
	 * @param name      a lifecycle method name
	 * @param codeRef   method's code reference
	 * @param startTime item start time
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildLifecycleMethodStartRq(@Nonnull final ItemType type, @Nonnull final String name,
			@Nullable final String codeRef, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(name);
		rq.setCodeRef(codeRef);
		rq.setStartTime(ofNullable(startTime).orElseGet(() -> Calendar.getInstance().getTime()));
		rq.setType(type.name());
		return rq;
	}

	/**
	 * Starts a test item on Report Portal
	 *
	 * @param parentId an id of a parent item
	 * @param rq       a request to Report Portal
	 * @return Report Portal item ID
	 */
	@Nonnull
	protected Maybe<String> startTestItem(@Nullable final Maybe<String> parentId, @Nonnull final StartTestItemRQ rq) {
		Launch myLaunch = launch.get();
		return ofNullable(parentId).map(p -> myLaunch.startTestItem(p, rq)).orElseGet(() -> myLaunch.startTestItem(rq));
	}

	/**
	 * Creates and starts a test item leaf
	 *
	 * @param type   the item type
	 * @param rq     a request to Report Portal
	 * @param parent a parent test item leaf
	 * @return a leaf of the item
	 */
	@Nonnull
	protected TestItemTree.TestItemLeaf createLeaf(@Nonnull final ItemType type, @Nonnull final StartTestItemRQ rq,
			@Nullable final TestItemTree.TestItemLeaf parent) {
		Optional<TestItemTree.TestItemLeaf> parentOptional = ofNullable(parent);
		Optional<Maybe<String>> parentId = parentOptional.map(TestItemTree.TestItemLeaf::getItemId);
		Maybe<String> itemId = startTestItem(parentId.orElse(null), rq);
		TestItemTree.TestItemLeaf l = parentId.map(p -> TestItemTree.createTestItemLeaf(p, itemId))
				.orElseGet(() -> TestItemTree.createTestItemLeaf(itemId));
		l.setType(type);
		l.setAttribute(START_TIME, rq.getStartTime());
		l.setAttribute(START_REQUEST, rq);
		parentOptional.ifPresent(p -> l.setAttribute(PARENT, p));
		ofNullable(rq.getCodeRef()).ifPresent(r -> l.setAttribute(CODE_REF, r));
		return l;
	}

	/**
	 * Retrieves a test item date from bypassed tree leaf and compares it with the current date. Returns current date if it newer than
	 * bypassed (parent) date or bypassed date in other case.
	 *
	 * @param parent a parent test item leaf
	 * @return current date or parent date
	 */
	@Nonnull
	protected Date getItemDate(@Nullable final TestItemTree.TestItemLeaf parent) {
		final Date previousDate = ofNullable(parent).map(p -> p.<Date>getAttribute(START_TIME))
				.orElseGet(() -> Calendar.getInstance().getTime());
		Date currentDate = Calendar.getInstance().getTime();
		Date itemDate;
		if (previousDate.compareTo(currentDate) <= 0) {
			itemDate = currentDate;
		} else {
			itemDate = previousDate;
		}
		return itemDate;
	}

	/**
	 * Returns current test item leaf in Test Tree. Creates Test Item Tree branches and leaves if no such items found.
	 *
	 * @return a leaf of an item inside ItemTree or null if not found
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected TestItemTree.TestItemLeaf retrieveLeaf() {
		final List<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> leafChain = new ArrayList<>();
		Entity<?> parentEntity = null;
		for (Entity<?> entity : structure) {
			final ItemType itemType = entity.type();
			final Optional<Pair<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf>> parent = leafChain.isEmpty() ?
					Optional.empty() :
					Optional.of(leafChain.get(leafChain.size() - 1));
			final TestItemTree.TestItemLeaf parentLeaf = parent.map(Pair::getValue).orElse(null);
			final Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = parent.map(p -> p.getValue()
					.getChildItems()).orElseGet(itemTree::getTestItems);
			final String parentCodeRef = parent.map(p -> (String) p.getValue().getAttribute(CODE_REF)).orElse(null);
			Date itemDate = getItemDate(parent.map(Pair::getValue).orElse(null));
			switch (itemType) {
				case STORY:
					Story story = (Story) entity.get();
					TestItemTree.ItemTreeKey storyKey = ItemTreeUtils.createKey(story);
					leafChain.add(ImmutablePair.of(storyKey,
							children.computeIfAbsent(storyKey, k -> createLeaf(ItemType.STORY,
									buildStartStoryRq(story, getCodeRef(parentCodeRef, k, ItemType.STORY), itemDate),
									parentLeaf
							))
					));
					break;
				case SCENARIO:
					Scenario scenario = (Scenario) entity.get();
					if (scenario.hasExamplesTable() && !scenario.getExamplesTable().getRows().isEmpty()) {
						// Each Example has its own scenario node, no need to wrap them into suite once again
						break;
					}
					TestItemTree.ItemTreeKey scenarioKey = ItemTreeUtils.createKey(getScenarioName(scenario));
					leafChain.add(ImmutablePair.of(scenarioKey, children.computeIfAbsent(scenarioKey, k -> createLeaf(
							ItemType.SCENARIO,
							buildStartScenarioRq(scenario, getCodeRef(parentCodeRef, k, ItemType.SCENARIO), itemDate),
							parentLeaf
					))));
					break;
				case SUITE: // type SUITE == an Example
					if (parentEntity == null) {
						LOGGER.error("Unable to locate Scenario item for Example, this is not something which is supposed to happen,"
								+ " skipping reporting");
						break;
					}
					Scenario parentScenario = (Scenario) parentEntity.get();
					Map<String, String> example = (Map<String, String>) entity.get();
					TestItemTree.ItemTreeKey parentScenarioKey = ItemTreeUtils.createKey(getScenarioName(parentScenario));
					TestItemTree.ItemTreeKey exampleKey = ItemTreeUtils.createKey(example);
					leafChain.add(ImmutablePair.of(exampleKey, children.computeIfAbsent(exampleKey, k -> {
						String parentScenarioCodeRef = getCodeRef(parentCodeRef, parentScenarioKey, ItemType.SCENARIO);
						TestItemTree.TestItemLeaf leaf = createLeaf(ItemType.SUITE,
								buildStartExampleRq(parentScenario, example, getCodeRef(parentScenarioCodeRef, k, ItemType.SUITE), itemDate),
								parentLeaf
						);
						leaf.setAttribute(PARAMETERS, example);
						return leaf;
					})));
					break;
				case TEST: // type TEST == a lifecycle SUITE
					String lifecycleSuiteName = (String) entity.get();
					TestItemTree.ItemTreeKey lifecycleSuiteKey = ItemTreeUtils.createKey(lifecycleSuiteName);
					leafChain.add(ImmutablePair.of(lifecycleSuiteKey,
							children.computeIfAbsent(lifecycleSuiteKey, k -> createLeaf(itemType,
									buildLifecycleSuiteStartRq(lifecycleSuiteName,
											getCodeRef(parentCodeRef, k, ItemType.TEST),
											itemDate
									),
									parentLeaf
							))
					));
					break;
			}
			parentEntity = entity;
		}
		return leafChain.isEmpty() ? null : leafChain.get(leafChain.size() - 1).getValue();
	}

	/**
	 * Returns current test item leaf in Test Tree.
	 *
	 * @return a leaf of an item inside ItemTree or null if not found
	 */
	@SuppressWarnings("unchecked")
	@Nullable
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
					Scenario scenario = (Scenario) entity.get();
					if (scenario.hasExamplesTable() && !scenario.getExamplesTable().getRows().isEmpty()) {
						// Each Example has its own scenario node, no need to wrap them into suite once again
						continue;
					}
					TestItemTree.ItemTreeKey scenarioKey = ItemTreeUtils.createKey(getScenarioName(scenario));
					leafChain.add(ImmutablePair.of(scenarioKey, children.get(scenarioKey)));
					break;
				case SUITE: // type SUITE == an Example
					TestItemTree.ItemTreeKey exampleKey = ItemTreeUtils.createKey((Map<String, String>) entity.get());
					leafChain.add(ImmutablePair.of(exampleKey, children.get(exampleKey)));
					break;
				default:
					TestItemTree.ItemTreeKey stepKey = ItemTreeUtils.createKey((String) entity.get());
					leafChain.add(ImmutablePair.of(stepKey, children.get(stepKey)));
					break;
			}
		}
		return leafChain.isEmpty() ? null : leafChain.get(leafChain.size() - 1).getValue();
	}

	/**
	 * Starts a scenario step on Report Portal
	 *
	 * @param name   a step name
	 * @param parent a parent test item leaf
	 * @return the step leaf
	 */
	protected TestItemTree.TestItemLeaf startStep(@Nonnull final String name,
			@Nonnull final TestItemTree.TestItemLeaf parent) {
		TestItemTree.ItemTreeKey key = ItemTreeUtils.createKey(name);
		TestItemTree.TestItemLeaf leaf = createLeaf(ItemType.STEP, buildStartStepRq(name,
				getCodeRef(parent.getAttribute(CODE_REF), key, ItemType.STEP),
				parent.getAttribute(PARAMETERS),
				getItemDate(parent)
		), parent);
		parent.getChildItems().put(key, leaf);
		return leaf;
	}

	/**
	 * Starts a lifecycle step on Report Portal
	 *
	 * @param name     a step name
	 * @param itemType lifecycle item type
	 * @param parent   a parent test item leaf
	 * @return the step leaf
	 */
	@Nonnull
	protected TestItemTree.TestItemLeaf startLifecycleMethod(@Nonnull final String name,
			@Nonnull final ItemType itemType, @Nonnull final TestItemTree.TestItemLeaf parent) {
		TestItemTree.ItemTreeKey key = ItemTreeUtils.createKey(name);
		String codeRef = getCodeRef(parent.getAttribute(CODE_REF), key, itemType);
		TestItemTree.TestItemLeaf leaf = createLeaf(itemType,
				buildLifecycleMethodStartRq(itemType, name, codeRef, getItemDate(parent)),
				parent
		);
		parent.getChildItems().put(key, leaf);
		return leaf;
	}

	/**
	 * Build finish test item request object
	 *
	 * @param id     item ID reference
	 * @param status item result status
	 * @param issue  test item issue to set (if any)
	 * @return finish request
	 */
	@SuppressWarnings("unused")
	@Nonnull
	protected FinishTestItemRQ buildFinishTestItemRequest(@Nonnull final Maybe<String> id,
			@Nullable final ItemStatus status, @Nullable Issue issue) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(ofNullable(status).map(Enum::name).orElse(null));
		rq.setIssue(issue);
		return rq;
	}

	/**
	 * Finishes an item in the structure
	 *
	 * @param item   an item to finish
	 * @param status a status to set on finish
	 */
	protected void finishItem(@Nullable final TestItemTree.TestItemLeaf item, @Nullable final ItemStatus status) {
		ofNullable(item).ifPresent(i -> {
			FinishTestItemRQ rq = buildFinishTestItemRequest(i.getItemId(), status, null);
			Maybe<OperationCompletionRS> response = launch.get().finishTestItem(i.getItemId(), rq);
			i.setStatus(status);
			i.setFinishResponse(response);
			i.setAttribute(FINISH_REQUEST, rq);
		});
	}

	/**
	 * Finishes the last item in the structure
	 *
	 * @param status a status to set on finish
	 */
	@SuppressWarnings("SameParameterValue")
	protected void finishLastItem(@Nullable final ItemStatus status) {
		TestItemTree.TestItemLeaf item = getLeaf();
		finishItem(item, status);
		structure.pollLast();
	}

	/**
	 * Calculate an Item status according to its child item status and current status. E.G.: SUITE-TEST or TEST-STEP.
	 *
	 * @param currentStatus an Item status
	 * @param childStatus   a status of its child element
	 * @return new status
	 * @see StatusEvaluation#evaluateStatus(ItemStatus, ItemStatus)
	 */
	@Nullable
	protected ItemStatus evaluateStatus(@Nullable final ItemStatus currentStatus,
			@Nullable final ItemStatus childStatus) {
		return StatusEvaluation.evaluateStatus(currentStatus, childStatus);
	}

	/**
	 * Pulls the last item in the structure stack, evaluates it status by child element statuses and finish it
	 */
	protected void evaluateAndFinishLastItem() {
		TestItemTree.TestItemLeaf item = getLeaf();
		Entity<?> entity = structure.pollLast();
		if (entity != null && ItemType.SCENARIO == entity.type()) {
			Scenario scenario = (Scenario) entity.get();
			if (scenario.hasExamplesTable() && !scenario.getExamplesTable().getRows().isEmpty()) {
				// Each Example has its own scenario node, no need to wrap them into suite once again
				return;
			}
		}
		ofNullable(item).ifPresent(i -> {
			ItemStatus status = i.getStatus();
			for (Map.Entry<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> entry : i.getChildItems().entrySet()) {
				TestItemTree.TestItemLeaf value = entry.getValue();
				if (value != null) {
					status = evaluateStatus(status, value.getStatus());
				}
			}
			i.setStatus(status);
			finishItem(i, status);
		});
	}

	/**
	 * Prepare a function which creates a {@link SaveLogRQ} from a {@link Throwable}
	 *
	 * @param level   a log level
	 * @param message a message to attach
	 * @return a {@link SaveLogRQ} supplier {@link Function}
	 */
	@Nonnull
	protected Function<String, SaveLogRQ> getLogSupplier(@Nonnull final LogLevel level,
			@Nullable final String message) {
		return itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel(level.name());
			rq.setLogTime(Calendar.getInstance().getTime());
			rq.setMessage(message);
			rq.setLogTime(Calendar.getInstance().getTime());
			return rq;
		};
	}

	/**
	 * Send a message to report portal about appeared failure
	 *
	 * @param itemId an ID of an Item to which bypassed stacktrace will be attached
	 * @param thrown {@link Throwable} object with details of the failure
	 */
	protected void sendStackTraceToRP(@Nonnull Maybe<String> itemId, @Nullable final Throwable thrown) {
		ofNullable(thrown).ifPresent(t -> ReportPortal.emitLog(itemId,
				getLogSupplier(LogLevel.ERROR, ExceptionUtils.getStackTrace(t))
		));
	}

	/**
	 * Finishes a test item on Report Portal
	 *
	 * @param id     an ID of an Item
	 * @param status a status of the item which will be set
	 * @param issue  an optional issue which will be set
	 */
	protected void finishItem(final @Nonnull Maybe<String> id, final @Nonnull ItemStatus status,
			@Nullable Issue issue) {
		FinishTestItemRQ rq = buildFinishTestItemRequest(id, status, issue);
		//noinspection ReactiveStreamsUnusedPublisher
		launch.get().finishTestItem(id, rq);
	}

	private void finishStep(final @Nonnull TestItemTree.TestItemLeaf step, final @Nonnull ItemStatus status,
			@Nullable Issue issue) {
		finishItem(step.getItemId(), status, issue);
		step.setStatus(status);
	}

	private void finishStep(final @Nonnull TestItemTree.TestItemLeaf step, final @Nonnull ItemStatus status) {
		finishStep(step, status, null);
	}

	/**
	 * Extension point to customize ignored steps insides
	 *
	 * @param step a step name
	 * @param leaf the step test item leaf
	 */
	@SuppressWarnings("unused")
	protected void createIgnoredSteps(@Nullable String step, @Nonnull TestItemTree.TestItemLeaf leaf) {
	}

	/**
	 * Extension point to customize a not performed step insides
	 *
	 * @param step a step name
	 * @param leaf the step test item leaf
	 */
	@SuppressWarnings("unused")
	protected void createNotPerformedSteps(@Nullable String step, @Nonnull TestItemTree.TestItemLeaf leaf) {
		ReportPortal.emitLog(leaf.getItemId(),
				getLogSupplier(LogLevel.WARN, "Step execution was skipped by JBehave, see previous steps for errors.")
		);
	}

	/**
	 * Extension point to customize pending steps insides
	 *
	 * @param step a step name
	 * @param leaf the step test item leaf
	 */
	@SuppressWarnings("unused")
	protected void createPendingSteps(@Nullable String step, @Nonnull TestItemTree.TestItemLeaf leaf) {
		ReportPortal.emitLog(leaf.getItemId(),
				getLogSupplier(LogLevel.WARN, String.format("Unable to locate a step implementation: '%s'", step))
		);
	}

	protected void simulateStep(@Nonnull String step) {
		TestItemTree.TestItemLeaf item = retrieveLeaf();
		ofNullable(item).ifPresent(i -> {
			TestItemTree.TestItemLeaf leaf = startStep(step, i);
			finishStep(leaf, ItemStatus.SKIPPED);
		});
	}

	/**
	 * Starts story (test suite level) in ReportPortal
	 *
	 * @param story      JBehave story object
	 * @param givenStory whether this story is given
	 * @see <a href="https://jbehave.org/reference/latest/given-stories.html">Given Stories</a>
	 */
	@Override
	public void beforeStory(@Nonnull Story story, boolean givenStory) {
		currentLifecycleTopItemType = ItemType.AFTER_GROUPS;
		currentLifecycleItemType = ItemType.BEFORE_SUITE;
		structure.add(new Entity<>(ItemType.STORY, story));
	}

	/**
	 * Finishes story in ReportPortal
	 */
	@Override
	public void afterStory(boolean givenStory) {
		TestItemTree.TestItemLeaf previousItem = getLeaf();
		if (previousItem != null && previousItem.getType() == ItemType.TEST) {
			evaluateAndFinishLastItem();
		}
		evaluateAndFinishLastItem();
	}

	@Override
	public void storyCancelled(Story story, StoryDuration storyDuration) {
		finishLastItem(ItemStatus.SKIPPED);
	}

	/**
	 * Starts scenario in ReportPortal (test level)
	 *
	 * @param scenario JBehave scenario object
	 */
	@Override
	public void beforeScenario(@Nonnull Scenario scenario) {
		TestItemTree.TestItemLeaf previousItem = getLeaf();
		if (previousItem != null && previousItem.getType() == ItemType.TEST) {
			evaluateAndFinishLastItem();
		}
		currentLifecycleItemType = ItemType.BEFORE_TEST;
		structure.add(new Entity<>(ItemType.SCENARIO, scenario));
	}

	/**
	 * Finishes scenario in ReportPortal
	 */
	@Override
	public void afterScenario(Timing timing) {
		TestItemTree.TestItemLeaf previousItem = getLeaf();
		if (previousItem != null && previousItem.getType() == ItemType.TEST) {
			evaluateAndFinishLastItem();
		}
		currentLifecycleItemType = ItemType.AFTER_SUITE;
		evaluateAndFinishLastItem();
	}

	/**
	 * Starts step in ReportPortal (TestStep level)
	 *
	 * @param step Step to be reported
	 */
	@Override
	public void beforeStep(@Nonnull Step step) {
		TestItemTree.TestItemLeaf previousItem = getLeaf();
		if (previousItem != null && previousItem.getType() == ItemType.TEST) {
			// Finish Before/After methods
			evaluateAndFinishLastItem();
		}
		TestItemTree.TestItemLeaf parentItem = retrieveLeaf();
		if (parentItem == null) {
			// Before Stories
			structure.add(new Entity<>(ItemType.TEST,
					currentLifecycleTopItemType == null ? BEFORE_STORIES : AFTER_STORIES
			));
			if (currentLifecycleTopItemType == null) {
				currentLifecycleTopItemType = ItemType.BEFORE_GROUPS;
			}
		} else if (parentItem.getType() == ItemType.STORY) {
			// Before Story
			structure.add(new Entity<>(ItemType.TEST,
					currentLifecycleItemType == ItemType.BEFORE_SUITE ? BEFORE_STORY : AFTER_STORY
			));
		}
		if (parentItem == null) {
			ofNullable(retrieveLeaf()).map(i -> startLifecycleMethod(
					step.getStepAsString(),
					currentLifecycleTopItemType,
					i
			)).ifPresent(stepStack::add);
			return;
		}
		currentLifecycleItemType = ItemType.BEFORE_METHOD;
		TestItemTree.TestItemLeaf stepLeaf = ofNullable(retrieveLeaf()).map(l -> startStep(step.getStepAsString(), l))
				.orElse(null);
		stepStack.add(stepLeaf);
		if (stepLeaf != null) {
			lastStep = stepLeaf;
		}
	}

	@Override
	public void beforeExamples(List<String> steps, ExamplesTable table) {
		// do nothing we will handle it on 'example' method call
	}

	@Override
	public void example(Map<String, String> tableRow, int exampleIndex) {
		TestItemTree.TestItemLeaf previousItem = getLeaf();
		if (previousItem != null && (previousItem.getType() == ItemType.TEST
				|| previousItem.getType() == ItemType.SUITE)) {
			evaluateAndFinishLastItem();
		}
		structure.add(new Entity<>(ItemType.SUITE, tableRow)); // type SUITE is used for Examples
	}

	/**
	 * Finishes the last examples item
	 */
	@Override
	public void afterExamples() {
		TestItemTree.TestItemLeaf previousItem = getLeaf();
		if (previousItem != null && previousItem.getType() == ItemType.TEST) {
			evaluateAndFinishLastItem();
		}
		evaluateAndFinishLastItem();
	}

	private void finishBeforeAfterSuites(TestItemTree.TestItemLeaf item) {
		if (item.getType() == ItemType.BEFORE_GROUPS || item.getType() == ItemType.AFTER_GROUPS) {
			Entity<?> parentSuite = structure.getLast();
			evaluateAndFinishLastItem(); // Finish parent suite
			structure.add(parentSuite);
		}
	}

	/**
	 * Finishes step in ReportPortal
	 */
	@Override
	public void successful(String step) {
		currentLifecycleItemType = ItemType.AFTER_TEST;
		ofNullable(stepStack.pollLast()).ifPresent(s -> {
			finishStep(s, ItemStatus.PASSED);
			finishBeforeAfterSuites(s);
		});
	}

	/**
	 * Finishes a test step with a failed status
	 *
	 * @param step  a step name
	 * @param cause a reason of a failure
	 */
	@Override
	public void failed(String step, Throwable cause) {
		ofNullable(stepStack.pollLast()).ifPresent(i -> {
			sendStackTraceToRP(i.getItemId(), cause);
			finishStep(i, ItemStatus.FAILED);
			finishBeforeAfterSuites(i);
		});
	}

	/**
	 * Report ignored step
	 *
	 * @param step a step name
	 */
	@Override
	public void ignorable(String step) {
		ofNullable(stepStack.pollLast()).ifPresent(i -> {
			createIgnoredSteps(step, i);
			finishStep(i, ItemStatus.SKIPPED);
			finishBeforeAfterSuites(i);
		});
	}

	/**
	 * Report a not performed step
	 *
	 * @param step a step name
	 */
	@Override
	public void notPerformed(String step) {
		ofNullable(stepStack.pollLast()).ifPresent(i -> {
			createNotPerformedSteps(step, i);
			finishStep(i, ItemStatus.SKIPPED, Launch.NOT_ISSUE);
			finishBeforeAfterSuites(i);
		});
	}

	@Override
	public void pending(StepCreator.PendingStep step) {
		ofNullable(stepStack.pollLast()).ifPresent(i -> {
			createPendingSteps(step.stepAsString(), i);
			finishStep(i, ItemStatus.SKIPPED);
			finishBeforeAfterSuites(i);
		});
	}

	@Override
	public void scenarioExcluded(Scenario scenario, String filter) {
		if (null != scenario.getExamplesTable() && scenario.getExamplesTable().getRowCount() > 0) {
			beforeExamples(scenario.getSteps(), scenario.getExamplesTable());
			for (int i = 0; i < scenario.getExamplesTable().getRowCount(); i++) {
				example(scenario.getExamplesTable().getRow(i), i);
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
