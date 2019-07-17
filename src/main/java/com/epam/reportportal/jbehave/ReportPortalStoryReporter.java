/*
 * Copyright 2019 EPAM Systems
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
import org.jbehave.core.reporters.StoryReporter;

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
public class ReportPortalStoryReporter implements StoryReporter {

	@Override
	public void scenarioMeta(Meta meta) {
		JBehaveContext.getCurrentStory().setScenarioMeta(meta);
	}

	@Override
	public void storyNotAllowed(Story story, String filter) {

	}

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
	public void narrative(Narrative narrative) {

	}

	@Override
	public void lifecyle(Lifecycle lifecycle) {

	}

	@Override
	public void beforeScenario(String scenarioTitle) {
		JBehaveUtils.startScenario(scenarioTitle);
	}

	@Override
	public void afterScenario() {
		JBehaveUtils.finishScenario();
	}

	@Override
	public void beforeGivenStories() {

	}

	@Override
	public void givenStories(GivenStories givenStories) {

	}

	@Override
	public void givenStories(List<String> storyPaths) {

	}

	@Override
	public void afterGivenStories() {

	}

	@Override
	public void beforeStep(String step) {
		JBehaveUtils.startStep(step);
	}

	@Override
	public void example(Map<String, String> tableRow) {
		JBehaveContext.getCurrentStory().getExamples().nextExample();
	}

	@Override
	public void example(Map<String, String> tableRow, int exampleIndex) {

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
		JBehaveUtils.finishStep(Statuses.PASSED);

	}

	@Override
	public void ignorable(String step) {
        if (JBehaveContext.getCurrentStory().getCurrentStep() == null) {
            JBehaveUtils.startStep(step);
        }
        JBehaveUtils.finishStep(Statuses.SKIPPED);
	}

	@Override
	public void comment(String step) {

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
		JBehaveUtils.finishStep(Statuses.FAILED);
	}

	@Override
	public void failedOutcomes(String step, OutcomesTable table) {

	}

	@Override
	public void restarted(String step, Throwable cause) {

	}

	@Override
	public void restartedStory(Story story, Throwable cause) {

	}

	@Override
	public void dryRun() {

	}

	@Override
	public void pendingMethods(List<String> methods) {

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

	@Override
	public void beforeScenario(Scenario scenario) {

	}

	private void simulateStep(String step, String status) {
		JBehaveUtils.startStep(step);
		JBehaveUtils.finishStep(status);
	}

}
