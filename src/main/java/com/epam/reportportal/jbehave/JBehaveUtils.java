/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-jbehave
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.jbehave;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.jbehave.core.model.Meta;
import org.jbehave.core.model.Story;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.annotations.VisibleForTesting;
import rp.com.google.common.base.*;
import rp.com.google.common.collect.Iterables;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * Set of usefull utils related to JBehave -> ReportPortal integration
 *
 * @author Andrei Varabyeu
 */
class JBehaveUtils {

	private JBehaveUtils() {
		// static utilities class
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(JBehaveUtils.class);

	private static final int MAX_NAME_LENGTH = 256;

	private static final String KEY_VALUE_SEPARATOR = ":";

	private static final String META_PARAMETER_SEPARATOR = " ";

	@VisibleForTesting
	static final Pattern STEP_NAME_PATTERN = Pattern.compile("<(.*?)>");

	private static Supplier<Launch> RP = Suppliers.memoize(new Supplier<Launch>() {

		/* should no be lazy */
		private final Date startTime = Calendar.getInstance().getTime();

		@Override
		public Launch get() {
			ReportPortal rp = ReportPortal.builder().build();
			ListenerParameters parameters = rp.getParameters();

			StartLaunchRQ rq = new StartLaunchRQ();
			rq.setName(parameters.getLaunchName());
			rq.setStartTime(startTime);
			rq.setMode(parameters.getLaunchRunningMode());
			rq.setTags(parameters.getTags());
			rq.setDescription(parameters.getDescription());

			return rp.newLaunch(rq);
		}
	});

	/**
	 * Finishes JBehaveLaunch in ReportPortal
	 */
	public static void finishLaunch() {
		JBehaveUtils.makeSureAllItemsFinished(Statuses.FAILED);

		FinishExecutionRQ finishLaunchRq = new FinishExecutionRQ();
		finishLaunchRq.setEndTime(Calendar.getInstance().getTime());

		RP.get().finish(finishLaunchRq);
	}

	/**
	 * Starts story (test suite level) in ReportPortal
	 *
	 * @param story
	 */
	public static void startStory(Story story, boolean givenStory) {

		StartTestItemRQ rq = new StartTestItemRQ();

		Set<String> metaProperties = story.getMeta().getPropertyNames();
		Map<String, String> metaMap = new HashMap<String, String>(metaProperties.size());
		for (String metaProperty : metaProperties) {
			metaMap.put(metaProperty, story.getMeta().getProperty(metaProperty));
		}

		if (Strings.isNullOrEmpty(story.getDescription().asString())) {
			rq.setDescription(story.getDescription().asString() + "\n" + joinMeta(metaMap));
		}
		rq.setName(normalizeName(story.getName()));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("STORY");

		Maybe<String> storyId;
		JBehaveContext.Story currentStory;

		if (givenStory) {
			/*
			 * Given story means inner story. That's why we need to create
			 * new story and assign parent to it
			 */
			Maybe<String> currentScenario = JBehaveContext.getCurrentStory().getCurrentScenario();
			Maybe<String> parent = currentScenario != null ? currentScenario : JBehaveContext.getCurrentStory().getCurrentStoryId();
			storyId = RP.get().startTestItem(parent, rq);
			currentStory = new JBehaveContext.Story();
			currentStory.setParent(JBehaveContext.getCurrentStory());
			JBehaveContext.setCurrentStory(currentStory);
		} else {
			storyId = RP.get().startTestItem(rq);
			currentStory = JBehaveContext.getCurrentStory();
		}
		currentStory.setCurrentStoryId(storyId);
		currentStory.setStoryMeta(story.getMeta());
		LOGGER.debug("Starting Story in ReportPortal: {} {}", story.getName(), givenStory);

	}

	/**
	 * Finishes story in ReportPortal
	 */
	public static void finishStory() {

		JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();

		if (null == currentStory.getCurrentStoryId()) {
			return;
		}

		LOGGER.debug("Finishing story in ReportPortal: {}", currentStory.getCurrentStoryId());

		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(Statuses.PASSED);
		RP.get().finishTestItem(currentStory.getCurrentStoryId(), rq);

		currentStory.setCurrentStoryId(null);
		if (currentStory.hasParent()) {
			JBehaveContext.setCurrentStory(currentStory.getParent());
		}

	}

	/**
	 * Starts step in ReportPortal (TestStep level)
	 *
	 * @param step Step to be reported
	 */
	public static void startStep(String step) {

		JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();

		StartTestItemRQ rq = new StartTestItemRQ();

		if (currentStory.hasExamples() && currentStory.getExamples().hasStep(step)) {
			List<ParameterResource> parameters = new ArrayList<ParameterResource>();
			StringBuilder name = new StringBuilder();
			name.append("[")
					.append(currentStory.getExamples().getCurrentExample())
					.append("] ")
					.append(expandParameters(step, currentStory.getExamples().getCurrentExampleParams(), parameters));
			rq.setParameters(parameters);
			rq.setName(normalizeName(name.toString()));
		} else {
			rq.setName(normalizeName(step));
			rq.setDescription(joinMetas(currentStory.getStoryMeta(), currentStory.getScenarioMeta()));
		}

		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("STEP");
		LOGGER.debug("Starting Step in ReportPortal: {}", step);

		Maybe<String> stepId = RP.get().startTestItem(currentStory.getCurrentScenario(), rq);
		currentStory.setCurrentStep(stepId);

	}

	/**
	 * Finishes step in ReportPortal
	 *
	 * @param status Step status
	 */
	public static void finishStep(String status) {

		JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();

		if (null == currentStory.getCurrentStep()) {
			return;
		}
		LOGGER.debug("Finishing Step in ReportPortal: {}", currentStory.getCurrentStep());
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status);

		RP.get().finishTestItem(currentStory.getCurrentStep(), rq);
		currentStory.setCurrentStep(null);

	}

	/**
	 * Starts scenario in ReportPortal (test level)
	 *
	 * @param scenario
	 */
	public static void startScenario(String scenario) {

		LOGGER.debug("Starting Scenario in ReportPortal: {}", scenario);

		JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(normalizeName(expandParameters(scenario, metasToMap(currentStory.getStoryMeta(), currentStory.getScenarioMeta()),
				Collections.<ParameterResource>emptyList()
		)));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("SCENARIO");
		rq.setDescription(joinMetas(currentStory.getStoryMeta(), currentStory.getScenarioMeta()));

		Maybe<String> rs = RP.get().startTestItem(currentStory.getCurrentStoryId(), rq);
		currentStory.setCurrentScenario(rs);
	}

	/**
	 * Finishes scenario in ReportPortal
	 */
	public static void finishScenario(String status) {

		JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();
		if (null == currentStory.getCurrentScenario()) {
			return;
		}

		LOGGER.debug("finishing scenario in ReportPortal: {}");
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status);
		try {
			RP.get().finishTestItem(currentStory.getCurrentScenario(), rq);
		} finally {
			currentStory.setCurrentScenario(null);
		}
	}

	/**
	 * Finishes scenario in ReportPortal
	 */
	public static void finishScenario() {
		finishScenario(Statuses.PASSED);
	}

	/**
	 * Iterate over started test items cache and remove all not finished item
	 *
	 * @param status
	 */
	public static void makeSureAllItemsFinished(String status) {

		Deque<Maybe<String>> items = JBehaveContext.getItemsCache();
		Maybe<String> item;
		while (null != (item = items.poll())) {
			FinishTestItemRQ rq = new FinishTestItemRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			rq.setStatus(status);
			RP.get().finishTestItem(item, rq);

		}
	}

	/**
	 * Send stacktrace to ReportPortal
	 *
	 * @param cause
	 */
	public static void sendStackTraceToRP(final Throwable cause) {

		ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(String itemId) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setTestItemId(itemId);
				rq.setLevel("ERROR");
				rq.setLogTime(Calendar.getInstance().getTime());
				if (cause != null) {
					rq.setMessage(getStackTraceAsString(cause.getCause()));
				} else {
					rq.setMessage("Test has failed without exception");
				}
				rq.setLogTime(Calendar.getInstance().getTime());

				return rq;
			}
		});
	}

	private static String joinMeta(Meta meta) {

		if (null == meta) {
			return StringUtils.EMPTY;
		}
		Iterator<String> metaParametersIterator = meta.getPropertyNames().iterator();

		if (metaParametersIterator.hasNext()) {
			StringBuilder appendable = new StringBuilder();
			String firstParameter = metaParametersIterator.next();
			appendable.append(joinMeta(firstParameter, meta.getProperty(firstParameter)));
			while (metaParametersIterator.hasNext()) {
				String nextParameter = metaParametersIterator.next();
				appendable.append(META_PARAMETER_SEPARATOR);
				appendable.append(joinMeta(nextParameter, meta.getProperty(nextParameter)));
			}
			return appendable.toString();
		}
		return StringUtils.EMPTY;

	}

	private static Map<String, String> metaToMap(Meta meta) {
		if (null == meta) {
			return Collections.emptyMap();
		}
		Map<String, String> metaMap = new HashMap<String, String>(meta.getPropertyNames().size());
		for (String name : meta.getPropertyNames()) {
			metaMap.put(name, meta.getProperty(name));
		}
		return metaMap;

	}

	// TODO rename as join metas
	private static Map<String, String> metasToMap(Meta... metas) {
		if (null != metas && metas.length > 0) {
			Map<String, String> metaMap = new HashMap<String, String>();
			for (Meta meta : metas) {
				metaMap.putAll(metaToMap(meta));
			}
			return metaMap;
		} else {
			return Collections.emptyMap();
		}
	}

	private static String joinMeta(Map<String, String> metaParameters) {
		Iterator<Entry<String, String>> metaParametersIterator = metaParameters.entrySet().iterator();

		if (metaParametersIterator.hasNext()) {
			StringBuilder appendable = new StringBuilder();
			Entry<String, String> firstParameter = metaParametersIterator.next();
			appendable.append(joinMeta(firstParameter.getKey(), firstParameter.getValue()));
			while (metaParametersIterator.hasNext()) {
				Entry<String, String> nextParameter = metaParametersIterator.next();
				appendable.append(META_PARAMETER_SEPARATOR);
				appendable.append(joinMeta(nextParameter.getKey(), nextParameter.getValue()));
			}
			return appendable.toString();
		}
		return StringUtils.EMPTY;

	}

	private static String joinMeta(String key, String value) {
		if (null == value) {
			return key;
		}
		if (value.toLowerCase().startsWith("http")) {
			String text;
			if (value.toLowerCase().contains("jira")) {
				text = key + KEY_VALUE_SEPARATOR + StringUtils.substringAfterLast(value, "/");
			} else {
				text = key;
			}
			return wrapAsLink(value, text);
		} else {
			return key + KEY_VALUE_SEPARATOR + value;
		}

	}

	// TODO should be removed since RP doesn't render HTML in the description
	@Deprecated
	private static String wrapAsLink(String href, String text) {
		return new StringBuilder("<a href=\"").append(href).append("\">").append(text).append("</a>").toString();
	}

	private static String joinMetas(Meta... metas) {
		return Joiner.on(META_PARAMETER_SEPARATOR).join(Iterables.transform(Arrays.asList(metas), new Function<Meta, String>() {

			@Override
			public String apply(Meta input) {
				return joinMeta(input);
			}
		}));
	}

	@VisibleForTesting
	static String expandParameters(String stepName, Map<String, String> parameters, List<ParameterResource> parameterResources) {
		Matcher m = STEP_NAME_PATTERN.matcher(stepName);
		StringBuffer buffer = new StringBuffer();
		while (m.find()) {
			String key = m.group(1);
			if (parameters.containsKey(key)) {
				String value = parameters.get(key);
				m.appendReplacement(buffer, value);
				ParameterResource param = buildParameter(key, value);
				parameterResources.add(param);
			}
		}
		m.appendTail(buffer);
		return buffer.toString();
	}

	private static ParameterResource buildParameter(String key, String value) {
		ParameterResource parameterResource = new ParameterResource();
		parameterResource.setKey(key);
		parameterResource.setValue(value);
		return parameterResource;
	}

	private static String normalizeName(String string) {
		String name;
		if (Strings.isNullOrEmpty(string)) {
			name = "UNKNOWN";
		} else if (string.length() > MAX_NAME_LENGTH) {
			name = string.substring(0, MAX_NAME_LENGTH - 1);
		} else {
			name = string;
		}
		return name;

	}

}
