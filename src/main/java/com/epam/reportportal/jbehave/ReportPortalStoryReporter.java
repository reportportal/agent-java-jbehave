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

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.model.StoryDuration;
import org.jbehave.core.reporters.NullStoryReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static com.epam.reportportal.jbehave.JBehaveUtils.*;
import static rp.com.google.common.base.Strings.isNullOrEmpty;

/**
 * JBehave Reporter for reporting results into ReportPortal. Requires using
 */
public class ReportPortalStoryReporter extends NullStoryReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalStoryReporter.class);
    private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
    private static final String AGENT_PROPERTIES_FILE = "agent.properties";

    private final MemoizingSupplier<Launch> launch;

    public ReportPortalStoryReporter(final ReportPortal rp) {
        launch = createLaunch(rp);
    }

    protected void finishLaunch(final Launch myLaunch) {
        Deque<Maybe<String>> items = JBehaveContext.getItemsCache();
        Maybe<String> item;
        while (null != (item = items.poll())) {
            FinishTestItemRQ rq = new FinishTestItemRQ();
            rq.setEndTime(Calendar.getInstance().getTime());
            rq.setStatus(ItemStatus.FAILED.name());
            myLaunch.finishTestItem(item, rq);
        }
        FinishExecutionRQ rq = new FinishExecutionRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        myLaunch.finish(rq);
    }

    protected Thread getShutdownHook(final Launch myLaunch) {
        return new Thread(() -> finishLaunch(myLaunch));
    }

    protected StartLaunchRQ buildStartLaunchRQ(Date startTime, ListenerParameters parameters) {
        StartLaunchRQ rq = new StartLaunchRQ();
        rq.setName(parameters.getLaunchName());
        rq.setStartTime(startTime);
        rq.setMode(parameters.getLaunchRunningMode());
        rq.setAttributes(parameters.getAttributes());
        rq.setDescription(parameters.getDescription());
        rq.setRerun(parameters.isRerun());
        if (!isNullOrEmpty(parameters.getRerunOf())) {
            rq.setRerunOf(parameters.getRerunOf());
        }
        if (null != parameters.getSkippedAnIssue()) {
            ItemAttributesRQ skippedIssueAttribute = new ItemAttributesRQ();
            skippedIssueAttribute.setKey(SKIPPED_ISSUE_KEY);
            skippedIssueAttribute.setValue(parameters.getSkippedAnIssue().toString());
            skippedIssueAttribute.setSystem(true);
            rq.getAttributes().add(skippedIssueAttribute);
        }
        rq.getAttributes().addAll(SystemAttributesExtractor.extract(AGENT_PROPERTIES_FILE, JBehaveUtils.class.getClassLoader()));
        return rq;
    }

    /**
     * Returns a supplier which initialize a launch on the first 'get'.
     *
     * @return a supplier with a lazy-initialized {@link Launch} instance
     */
    protected MemoizingSupplier<Launch> createLaunch(final ReportPortal rp) {
        return new MemoizingSupplier<>(new Supplier<Launch>() {
            /* should no be lazy */
            private final Date startTime = Calendar.getInstance().getTime();

            @Override
            public Launch get() {
                ListenerParameters parameters = rp.getParameters();
                StartLaunchRQ rq = buildStartLaunchRQ(startTime, parameters);
                Launch myLaunch = rp.newLaunch(rq);
                Runtime.getRuntime().addShutdownHook(getShutdownHook(myLaunch));
                myLaunch.start();
                return myLaunch;
            }
        });
    }

    @Override
    public void storyCancelled(Story story, StoryDuration storyDuration) {
        finishStory();
    }

    private void finishStory() {
        JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();

        if (null == currentStory.getCurrentStoryId()) {
            return;
        }

        LOGGER.debug("Finishing story in ReportPortal: {}", currentStory.getCurrentStoryId());

        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(ItemStatus.PASSED.name());
        launch.get().finishTestItem(currentStory.getCurrentStoryId(), rq);

        currentStory.setCurrentStoryId(null);
        if (currentStory.hasParent()) {
            JBehaveContext.setCurrentStory(currentStory.getParent());
        }
    }

    /**
     * Starts story (test suite level) in ReportPortal
     *
     * @param story
     */
    @Override
    public void beforeStory(Story story, boolean givenStory) {
        StartTestItemRQ rq = new StartTestItemRQ();

        Set<String> metaProperties = story.getMeta().getPropertyNames();
        Map<String, String> metaMap = new HashMap<>(metaProperties.size());
        for (String metaProperty : metaProperties) {
            metaMap.put(metaProperty, story.getMeta().getProperty(metaProperty));
        }

        if (isNullOrEmpty(story.getDescription().asString())) {
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
            storyId = launch.get().startTestItem(parent, rq);
            currentStory = new JBehaveContext.Story();
            currentStory.setParent(JBehaveContext.getCurrentStory());
            JBehaveContext.setCurrentStory(currentStory);
        } else {
            storyId = launch.get().startTestItem(rq);
            currentStory = JBehaveContext.getCurrentStory();
        }
        currentStory.setCurrentStoryId(storyId);
        currentStory.setStoryMeta(story.getMeta());
        LOGGER.debug("Starting Story in ReportPortal: {} {}", story.getName(), givenStory);
    }

    /**
     * Finishes story in ReportPortal
     */
    @Override
    public void afterStory(boolean givenStory) {
        finishStory();
    }

    /**
     * Starts scenario in ReportPortal (test level)
     *
     * @param scenario
     */
    @Override
    public void beforeScenario(Scenario scenario) {
        JBehaveContext.getCurrentStory().setScenarioMeta(scenario.getMeta());

        LOGGER.debug("Starting Scenario in ReportPortal: {}", scenario);

        JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setName(normalizeName(expandParameters(
                scenario.getTitle(),
                metasToMap(currentStory.getStoryMeta(), currentStory.getScenarioMeta()),
                Collections.<ParameterResource>emptyList()
        )));
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType("SCENARIO");
        rq.setDescription(joinMetas(currentStory.getStoryMeta(), currentStory.getScenarioMeta()));

        Maybe<String> rs = launch.get().startTestItem(currentStory.getCurrentStoryId(), rq);
        currentStory.setCurrentScenario(rs);
    }

    /**
     * Finishes scenario in ReportPortal
     */
    protected void finishScenario(ItemStatus status) {
        JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();
        if (null == currentStory.getCurrentScenario()) {
            return;
        }

        LOGGER.debug("finishing scenario in ReportPortal");
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(status.name());
        try {
            launch.get().finishTestItem(currentStory.getCurrentScenario(), rq);
        } finally {
            currentStory.setCurrentScenario(null);
        }
    }

    /**
     * Finishes scenario in ReportPortal
     */
    @Override
    public void afterScenario() {
        finishScenario(ItemStatus.PASSED);
    }

    protected void startStep(String step) {
        JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();

        StartTestItemRQ rq = new StartTestItemRQ();

        if (currentStory.hasExamples() && currentStory.getExamples().hasStep(step)) {
            List<ParameterResource> parameters = new ArrayList<>();
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

        Maybe<String> stepId = launch.get().startTestItem(currentStory.getCurrentScenario(), rq);
        currentStory.setCurrentStep(stepId);
        currentStory.setCurrentStepStatus(ItemStatus.PASSED);
    }

    /**
     * Starts step in ReportPortal (TestStep level)
     *
     * @param step Step to be reported
     */
    @Override
    public void beforeStep(String step) {
        startStep(step);
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

    protected void finishStep(String step) {
        finishStep(step, JBehaveContext.getCurrentStory().getCurrentStepStatus());
    }

    protected void finishStep(String step, ItemStatus status) {
        JBehaveContext.Story currentStory = JBehaveContext.getCurrentStory();

        JBehaveContext.Step currentStep = currentStory.getCurrentStep();
        if (null == currentStep) {
            return;
        }
        LOGGER.debug("Finishing Step in ReportPortal: {}", currentStep.getStepId());
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(Calendar.getInstance().getTime());
        rq.setStatus(status.name());
        rq.setIssue(currentStep.getIssue());

        launch.get().finishTestItem(currentStep.getStepId(), rq);
        currentStory.setCurrentStep(null);
    }

    /**
     * Finishes step in ReportPortal
     */
    @Override
    public void successful(String step) {
        finishStep(step);
    }

    @Override
    public void ignorable(String step) {
        if (JBehaveContext.getCurrentStory().getCurrentStep() == null) {
            startStep(step);
        }
        finishStep(step, ItemStatus.SKIPPED);
    }

    @Override
    public void notPerformed(String step) {
        if (JBehaveContext.getCurrentStory().getCurrentStep() == null) {
            startStep(step);
        }
        finishStep(step, ItemStatus.SKIPPED);
    }

    @Override
    public void failed(String step, Throwable cause) {
        JBehaveUtils.sendStackTraceToRP(cause);
        finishStep(step, ItemStatus.FAILED);
    }

    @Override
    public void pending(String step) {
        simulateStep(step, ItemStatus.SKIPPED);
    }

    @Override
    public void scenarioNotAllowed(Scenario scenario, String filter) {
        if (null != scenario.getExamplesTable() && scenario.getExamplesTable().getRowCount() > 0) {
            beforeExamples(scenario.getSteps(), scenario.getExamplesTable());
            for (int i = 0; i < scenario.getExamplesTable().getRowCount(); i++) {
                example(scenario.getExamplesTable().getRow(i));
                for (String step : scenario.getSteps()) {
                    simulateStep(step, ItemStatus.SKIPPED);
                }
            }
        } else {
            for (String step : scenario.getSteps()) {
                simulateStep(step, ItemStatus.SKIPPED);
            }
        }
        finishScenario(ItemStatus.SKIPPED);
    }

    private void simulateStep(String step, ItemStatus status) {
        startStep(step);
        finishStep(step, status);
    }
}
