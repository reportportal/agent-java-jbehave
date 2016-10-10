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

import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Meta;

import java.util.*;

/**
 * JBehave test execution context
 *
 * @author Andrei Varabyeu
 *
 */
public class JBehaveContext {

	private static String currentLaunch;

	private static ThreadLocal<Story> currentStory = new ThreadLocal<JBehaveContext.Story>() {
		@Override
		protected Story initialValue() {
			return new Story();
		}
	};

	private static Deque<String> itemsCache = new LinkedList<String>();

	public static void setCurrentLaunch(String currentLaunch) {
		JBehaveContext.currentLaunch = currentLaunch;
	}

	public static String getCurrentLaunch() {
		return currentLaunch;
	}

	public static Story getCurrentStory() {
		return currentStory.get();
	}

	public static void setCurrentStory(Story story){
		currentStory.set(story);
	}

	public static Deque<String> getItemsCache() {
		return itemsCache;
	}

	public static class Story {

		private String currentStoryId;

		private String currentScenario;

		private String currentStep;

		private Examples examples;

		private Meta scenarioMeta;

		private Meta storyMeta;

		private Story parent;

		public boolean hasParent(){
			return null != parent;
		}

		public Story getParent() {
			return parent;
		}

		public void setParent(Story parent) {
			this.parent = parent;
		}

		/**
		 * @param currentStep
		 *            the currentStep to set
		 */
		public void setCurrentStep(String currentStep) {
			if (null != currentStep) {
				itemsCache.push(currentStep);
			} else {
				itemsCache.remove(this.currentStep);
			}
			this.currentStep = currentStep;
		}

		public void setCurrentStoryId(String currentStoryId) {
			if (null != currentStoryId) {
				itemsCache.push(currentStoryId);
			} else {
				itemsCache.remove(this.currentStoryId);
			}
			this.currentStoryId = currentStoryId;
		}

		public String getCurrentStoryId() {
			return currentStoryId;
		}

		/**
		 * @return the currentScenario
		 */
		public String getCurrentScenario() {
			return currentScenario;
		}

		/**
		 * @param currentScenario
		 *            the currentScenario to set
		 */
		public void setCurrentScenario(String currentScenario) {
			if (null != currentScenario) {
				itemsCache.push(currentScenario);
			} else {
				itemsCache.remove(this.currentScenario);
			}
			this.currentScenario = currentScenario;
		}

		/**
		 * @return the currentStep
		 */
		public String getCurrentStep() {
			return currentStep;
		}

		public void setExamples(Examples examples) {
			this.examples = examples;
		}

		public boolean hasExamples() {
			return null != examples;
		}

		public Examples getExamples() {
			return examples;
		}

		public void setScenarioMeta(Meta scenarioMeta) {
			this.scenarioMeta = scenarioMeta;
		}

		public Meta getScenarioMeta() {
			return scenarioMeta;
		}

		public void setStoryMeta(Meta storyMeta) {
			this.storyMeta = storyMeta;
		}

		public Meta getStoryMeta() {
			return storyMeta;
		}

	}

	public static class Examples {
		private ExamplesTable examplesTable;

		private int currentExample;

		private List<String> steps;

		Examples(List<String> steps, ExamplesTable examplesTable) {
			this.examplesTable = examplesTable;
			this.currentExample = -1;
			this.steps = steps;
		}

		public ExamplesTable getExamplesTable() {
			return examplesTable;
		}

		public Map<String, String> getCurrentExampleParams() {
			return examplesTable.getRow(currentExample);
		}

		public int getCurrentExample() {
			return currentExample;
		}

		public List<String> getSteps() {
			return steps;
		}

		public boolean hasStep(String step) {
			return steps.contains(step);
		}

		public void nextExample() {
			currentExample++;
		}

	}

}
