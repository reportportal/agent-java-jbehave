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

import io.reactivex.Maybe;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Meta;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static Map<Story, Deque<Maybe<String>>> itemsCache = new ConcurrentHashMap<>();
    private static Map<Story, Deque<Maybe<String>>> stepsCache = new ConcurrentHashMap<>();

    public static Story getCurrentStory() {
        return currentStory.get();
    }

    public static void setCurrentStory(Story story) {
        currentStory.set(story);
    }

    public static Deque<Maybe<String>> getItemsCache() {
        Deque<Maybe<String>> merged = new LinkedList<>();
        merged.addAll(mergeMapValues(stepsCache));
        merged.addAll(mergeMapValues(itemsCache));
        return merged;
    }

    private static Deque<Maybe<String>> mergeMapValues(Map<Story, Deque<Maybe<String>>> map) {
        Deque<Maybe<String>> merged = new LinkedList<>();
        for (Deque<Maybe<String>> value : map.values()) {
            merged.addAll(value);
        }
        return merged;
    }

    private static Deque<Maybe<String>> getEntryFrom(Map<Story, Deque<Maybe<String>>> cache, Story story) {
        Deque<Maybe<String>> entry = cache.get(story);
        if (entry == null) {
            entry = new LinkedList<>();
            cache.put(story, entry);
        }
        return entry;
    }

    private static Deque<Maybe<String>> updateCache(Map<Story, Deque<Maybe<String>>> cache, Story story,
            Maybe<String> currentItem, Maybe<String> item) {
        Deque<Maybe<String>> cacheEntry = getEntryFrom(cache, story);
        if (null != item) {
            cacheEntry.push(item);
        } else {
            cacheEntry.remove(currentItem);
        }
        return cacheEntry;
    }

    public static class Story {

        private Maybe<String> currentStoryId;

        private Maybe<String> currentScenario;

        private Maybe<String> currentStep;
        private String currentStepStatus;

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
            Deque<Maybe<String>> cacheEntry = updateCache(stepsCache, this, this.currentStep, currentStep);
            this.currentStep = cacheEntry.peek();
        }

        public void setCurrentStoryId(Maybe<String> currentStoryId) {
            updateCache(itemsCache, this, this.currentStoryId, currentStoryId);
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
            updateCache(itemsCache, this, this.currentScenario, currentScenario);
            this.currentScenario = currentScenario;
        }

        /**
         * @return the currentStep
         */
        public Maybe<String> getCurrentStep() {
            return currentStep;
        }

        public String getCurrentStepStatus() {
            return currentStepStatus;
        }

        public void setCurrentStepStatus(String currentStepStatus) {
            this.currentStepStatus = currentStepStatus;
        }

        public void setExamples(Examples examples) {
            this.examples = examples;
        }

        public boolean hasExamples() {
            return null != examples && examples.getExamplesTable().getRowCount() > 0;
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
        private final ExamplesTable examplesTable;

        private int currentExample;

        private final List<String> steps;

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

        public void setCurrentExample(int currentExample) {
            this.currentExample = currentExample;
        }

    }

}
