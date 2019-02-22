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

import com.epam.reportportal.listeners.Statuses;
import org.jbehave.core.model.*;
import org.jbehave.core.reporters.NullStoryReporter;

import java.util.List;
import java.util.Map;

/**
 * JBehave Reporter for reporting results into ReportPortal. Requires using
 * {@link ReportPortalEmbeddable} or
 * {@link RpJUnitStories} as well
 * 
 * @author Andrei Varabyeu
 * 
 */
public class ReportPortalStoryReporter extends NullStoryReporter {

	@Override
	public void storyCancelled(Story story, StoryDuration storyDuration) {
		JBehaveUtils.finishStory();
	}

	@Override
	public void beforeStory(Story story, boolean givenStory) {
		JBehaveUtils.startStory(story, givenStory);
	}

	@Override
	public void afterStory(boolean givenStory) {
		JBehaveUtils.finishStory();
	}

	@Override
	public void beforeScenario(Scenario scenario) {
		JBehaveContext.getCurrentStory().setScenarioMeta(scenario.getMeta());
		JBehaveUtils.startScenario(scenario.getTitle());
	}

	@Override
	public void afterScenario() {
		JBehaveUtils.finishScenario();
	}

	@Override
	public void beforeStep(String step) {
		JBehaveUtils.startStep(step);
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

	@Override
	public void successful(String step) {
		JBehaveUtils.finishStep();
	}

	@Override
	public void ignorable(String step) {
        if (JBehaveContext.getCurrentStory().getCurrentStep() == null) {
            JBehaveUtils.startStep(step);
        }
        JBehaveUtils.finishStep(Statuses.SKIPPED);
	}

	@Override
	public void notPerformed(String step) {
        if (JBehaveContext.getCurrentStory().getCurrentStep() == null) {
            JBehaveUtils.startStep(step);
        }
		JBehaveUtils.finishStep(Statuses.SKIPPED);
	}

	@Override
	public void failed(String step, Throwable cause) {
        JBehaveUtils.sendStackTraceToRP(cause);
        JBehaveUtils.finishStep(Statuses.FAILED);
	}

	@Override
	public void pending(String step) {
		simulateStep(step, Statuses.SKIPPED);
	}

	@Override
	public void scenarioNotAllowed(Scenario scenario, String filter) {
		if (null != scenario.getExamplesTable() && scenario.getExamplesTable().getRowCount() > 0) {
			beforeExamples(scenario.getSteps(), scenario.getExamplesTable());
			for (int i = 0; i < scenario.getExamplesTable().getRowCount(); i++) {
				example(scenario.getExamplesTable().getRow(i));
				for (String step : scenario.getSteps()) {
					simulateStep(step, Statuses.SKIPPED);
				}
			}
		} else {
			for (String step : scenario.getSteps()) {
				simulateStep(step, Statuses.SKIPPED);
			}
		}
		JBehaveUtils.finishScenario(Statuses.SKIPPED);
	}

	private void simulateStep(String step, String status) {
		JBehaveUtils.startStep(step);
		JBehaveUtils.finishStep(status);
	}

}
