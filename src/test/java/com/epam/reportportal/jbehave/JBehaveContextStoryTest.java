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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.epam.reportportal.jbehave.JBehaveContext.Story;

import org.jbehave.core.model.ExamplesTable;
import org.junit.After;
import org.junit.Test;

import io.reactivex.Maybe;

/**
 * @author Valery Yatsynovich
 */
public class JBehaveContextStoryTest {

    private final JBehaveContext.Story story = new JBehaveContext.Story();

    @After
    public void tearDown() {
        story.setCurrentStep(null);
        story.setCurrentScenario(null);
        story.setCurrentStoryId(null);
        setEmptyMap("itemsCache");
        setEmptyMap("stepsCache");
    }

    @Test
    public void testStoryHasNoExamplesWhenExamplesTableIsEmpty() {
        JBehaveContext.Story story = new JBehaveContext.Story();
        story.setExamples(new JBehaveContext.Examples(Collections.<String>emptyList(), ExamplesTable.EMPTY));
        assertFalse(story.hasExamples());
    }

    @Test
    public void testItemsCache() {
        String storyId = "storyId";
        String scenario = "scenario";
        String step = "step";
        story.setCurrentStoryId(Maybe.just(storyId));
        story.setCurrentScenario(Maybe.just(scenario));
        story.setCurrentStep(Maybe.just(step));
        Deque<Maybe<String>> itemsCache = JBehaveContext.getItemsCache();
        assertEquals(3, itemsCache.size());
        Iterator<Maybe<String>> iterator = itemsCache.iterator();
        assertEquals(step, iterator.next().blockingGet());
        assertEquals(scenario, iterator.next().blockingGet());
        assertEquals(storyId, iterator.next().blockingGet());
    }

    @Test
    public void testFullStoryExecution() {
        story.setCurrentStoryId(Maybe.just("storyId"));
        story.setCurrentScenario(Maybe.just("scenario"));
        story.setCurrentStep(Maybe.just("step"));
        story.setCurrentStep(null);
        story.setCurrentScenario(null);
        story.setCurrentStoryId(null);
        assertEquals(0, JBehaveContext.getItemsCache().size());
    }

    @Test
    public void testGetCurrentStep() throws Throwable {
        int tasksCount = 500;
        List<Callable<Void>> tasks = new ArrayList<>(tasksCount);
        for (int taskIndex = 0; taskIndex < tasksCount; taskIndex++) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Story currentStory = JBehaveContext.getCurrentStory();
                    String step = String.valueOf(ThreadLocalRandom.current().nextLong());
                    Maybe<String> maybe = Maybe.just(step);
                    for (int count = 0; count < 50; count++) {
                        currentStory.setCurrentStep(maybe);
                    }
                    assertEquals(maybe, currentStory.getCurrentStep().getStepId());
                    return null;
                }
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.awaitTermination(0, TimeUnit.SECONDS);
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw e.getCause();
            }
        }
    }

    private void setEmptyMap(String cacheField) {
        try {
            Field field = JBehaveContext.class.getDeclaredField(cacheField);
            field.setAccessible(true);
            field.set(null, new ConcurrentHashMap<Story, Deque<Maybe<String>>>());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
