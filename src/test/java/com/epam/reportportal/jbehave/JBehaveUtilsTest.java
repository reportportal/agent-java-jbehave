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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import rp.com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for JBehaveUtils
 *
 * @author Andrei Varabyeu
 */
public class JBehaveUtilsTest {

	private static final Map<String, Pattern> predefinedProperties = new HashMap<>();

	@BeforeClass
	public static void initKeys() {
		predefinedProperties.put("os", Pattern.compile("^.+\\|.+\\|.+$"));
		predefinedProperties.put("jvm", Pattern.compile("^.+\\|.+\\|.+$"));
		predefinedProperties.put("agent", Pattern.compile("^agent-java-jbehave\\|.+$"));
	}

	public static final String STEP_NAME = "Given that I am on the <parameter1> page with <parameter2> value";

	@Test
	public void testStepParametersPattern() {
		Matcher m = JBehaveUtils.STEP_NAME_PATTERN.matcher(STEP_NAME);
		List<String> results = new ArrayList<>();
		while (m.find()) {
			results.add(m.group(1));
		}
		Assert.assertThat("Incorrect pattern", results, Matchers.contains("parameter1", "parameter2"));
	}

	@Test
	public void testPredefinedAttributes() {
		final ListenerParameters parameters = new ListenerParameters();
		parameters.setSkippedAnIssue(null);
		final StartLaunchRQ startLaunchRQ = JBehaveUtils.buildStartLaunchRQ(Calendar.getInstance().getTime(), parameters);
		assertThat(startLaunchRQ.getAttributes().size(), Matchers.is(3));
		Set<String> keys = startLaunchRQ.getAttributes().stream().map(ItemAttributeResource::getKey).collect(toSet());
		predefinedProperties.forEach((predefinedKey, predefinedValue) -> assertTrue(keys.contains(predefinedKey)));
		startLaunchRQ.getAttributes().forEach(attribute -> {
			assertTrue(predefinedProperties.get(attribute.getKey()).matcher(attribute.getValue()).matches());
			assertTrue(attribute.isSystem());
		});
	}

	@Test
	public void testStepParametersReplacement() {
		List<ParameterResource> params = new ArrayList<>();
		String result = JBehaveUtils.expandParameters(STEP_NAME,
				ImmutableMap.<String, String> builder().put("parameter1", "repl1").put("parameter2", "repl2").build(), params);
		Assert.assertThat("Incorrect parameters replacement", result, Matchers.is("Given that I am on the repl1 page with repl2 value"));
		Assert.assertEquals("Incorrect parameters key expand", "parameter1", params.get(0).getKey());
		Assert.assertEquals("Incorrect parameters value expand", "repl1", params.get(0).getValue());
		Assert.assertEquals("Incorrect parameters key expand", "parameter2", params.get(1).getKey());
		Assert.assertEquals("Incorrect parameters value expand", "repl2", params.get(1).getValue());
	}

	@Test
	public void testStepParametersReplacementWithSpecialChars() {
		List<ParameterResource> params = new ArrayList<>();
		String result = JBehaveUtils.expandParameters("Given I am on the <parameter1> page",
				ImmutableMap.of("parameter1", "${repl1}"), params);
		Assert.assertThat("Incorrect parameters replacement", result, Matchers.is("Given I am on the ${repl1} page"));
		Assert.assertEquals("Incorrect parameters key expand", "parameter1", params.get(0).getKey());
		Assert.assertEquals("Incorrect parameters value expand", "${repl1}", params.get(0).getValue());
	}

	@Test
	public void testStepParametersReplacementNegative() {
		List<ParameterResource> params = new ArrayList<>();
		String result = JBehaveUtils.expandParameters("Given that I am on",
				ImmutableMap.<String, String> builder().put("parameter1", "repl1").put("parameter2", "repl2").build(), params);
		Assert.assertThat("Incorrect parameters replacement", result, Matchers.is("Given that I am on"));
		Assert.assertThat("Incorrect parameters expand", params, Matchers.<ParameterResource>empty());
	}
}
