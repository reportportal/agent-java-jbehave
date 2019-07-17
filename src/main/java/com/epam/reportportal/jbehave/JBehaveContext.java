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

import io.reactivex.Maybe;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Meta;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * JBehave test execution context
 *
 * @author Andrei Varabyeu
 */
public class JBehaveContext {

	private static ThreadLocal<Story> currentStory = new ThreadLocal<JBehaveContext.Story>() {
		@Override
		protected Story initialValue() {
			return new Story();
		}
	};

	private static Deque<Maybe<String>> itemsCache = new LinkedList<>();

	public static Story getCurrentStory() {
		return currentStory.get();
	}

	public static void setCurrentStory(Story story) {
		currentStory.set(story);
	}

	public static Deque<Maybe<String>> getItemsCache() {
		return itemsCache;
	}

	public static class Story {

		private Maybe<String> currentStoryId;

		private Maybe<String> currentScenario;

		private Maybe<String> currentStep;

		private Examples examples;

		private Meta scenarioMeta;

		private Meta storyMeta;

		private Story parent;

		public boolean hasParent() {
			return null != parent;
		}

		public Story getParent() {
			return parent;
		}

		public void setParent(Story parent) {
			this.parent = parent;
		}

		/**
		 * @param currentStep the currentStep to set
		 */
		public void setCurrentStep(Maybe<String> currentStep) {
			if (null != currentStep) {
				itemsCache.push(currentStep);
			} else {
				itemsCache.remove(this.currentStep);
			}
			this.currentStep = currentStep;
		}

		public void setCurrentStoryId(Maybe<String> currentStoryId) {
			if (null != currentStoryId) {
				itemsCache.push(currentStoryId);
			} else {
				itemsCache.remove(this.currentStoryId);
			}
			this.currentStoryId = currentStoryId;
		}

		public Maybe<String> getCurrentStoryId() {
			return currentStoryId;
		}

		/**
		 * @return the currentScenario
		 */
		public Maybe<String> getCurrentScenario() {
			return currentScenario;
		}

		/**
		 * @param currentScenario the currentScenario to set
		 */
		public void setCurrentScenario(Maybe<String> currentScenario) {
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
		public Maybe<String> getCurrentStep() {
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
