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

package com.epam.reportportal.jbehave.integration.basic;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static java.util.Optional.ofNullable;

public class NestedStepsStepReporterSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(NestedStepsStepReporterSteps.class);
	public static final String FIRST_NAME = "I am the first nested step";
	public static final String SECOND_NAME = "I am the second nested step";
	public static final String THIRD_NAME = "I am the third nested step";
	public static final String FIRST_NESTED_STEP_LOG = "Inside first nested step";
	public static final String SECOND_NESTED_STEP_LOG = "Inside second nested step";
	public static final String THIRD_NESTED_STEP_LOG = "Third error log of the second step";
	public static final String DURING_SECOND_NESTED_STEP_LOG = "A log entry during the first nested step report";

	private static final IllegalStateException NO_LAUNCH_EXCEPTION = new IllegalStateException("Unable to get Launch");

	@SuppressWarnings("unused")
	@Given("a step with a manual step")
	public void i_have_a_step_with_a_manual_step() {
		StepReporter stepReporter = ofNullable(Launch.currentLaunch()).map(Launch::getStepReporter).orElseThrow(() -> NO_LAUNCH_EXCEPTION);

		stepReporter.sendStep(FIRST_NAME);
		LOGGER.info(FIRST_NESTED_STEP_LOG);
	}

	@SuppressWarnings("unused")
	@Then("a step with two manual steps")
	public void i_have_a_step_with_two_manual_steps() {
		StepReporter stepReporter = ofNullable(Launch.currentLaunch()).map(Launch::getStepReporter).orElseThrow(() -> NO_LAUNCH_EXCEPTION);

		stepReporter.sendStep(SECOND_NAME, DURING_SECOND_NESTED_STEP_LOG);
		LOGGER.info(SECOND_NESTED_STEP_LOG);

		stepReporter.sendStep(ItemStatus.FAILED, THIRD_NAME, new File("pug/unlucky.jpg"));
		LOGGER.error(THIRD_NESTED_STEP_LOG);
	}

}
