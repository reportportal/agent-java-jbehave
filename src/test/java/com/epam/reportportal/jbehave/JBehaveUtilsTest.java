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

import com.epam.ta.reportportal.ws.model.ParameterResource;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import rp.com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Some unit tests for JBehaveUtils
 *
 * @author Andrei Varabyeu
 */
public class JBehaveUtilsTest {
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
	public void testStepParametersReplacementNegative() {
		List<ParameterResource> params = new ArrayList<>();
		String result = JBehaveUtils.expandParameters("Given that I am on",
				ImmutableMap.<String, String> builder().put("parameter1", "repl1").put("parameter2", "repl2").build(), params);
		Assert.assertThat("Incorrect parameters replacement", result, Matchers.is("Given that I am on"));
		Assert.assertThat("Incorrect parameters expand", params, Matchers.<ParameterResource>empty());
	}
}
