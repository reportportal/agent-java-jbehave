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
package com.epam.reportportal.jbehave;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.MemoizingSupplier;
import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.jbehave.core.reporters.FilePrintStreamFactory;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporter;
import org.jbehave.core.reporters.StoryReporterBuilder;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * A common class for ReportPortal formats, which are for reporting JBehave runs into Report Portal application.
 *
 * @author Vadzim Hushchanskou
 */
public abstract class ReportPortalFormat extends Format {

	private static final ThreadLocal<ReportPortalFormat> INSTANCES = new InheritableThreadLocal<>();
	private static final ThreadLocal<ReportPortalStoryReporter> STORY_REPORTERS = new InheritableThreadLocal<>();

	private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	private static final String AGENT_PROPERTIES_FILE = "agent.properties";

	protected final MemoizingSupplier<Launch> launch;
	protected final TestItemTree itemTree = new TestItemTree();
	protected final ReportPortal rp;

	/**
	 * Creates an instance of the formatter class using specific {@link ReportPortal} reporter.
	 *
	 * @param reportPortal an instance of Report Portal reporter
	 */
	public ReportPortalFormat(final ReportPortal reportPortal) {
		super("REPORT_PORTAL");
		rp = reportPortal;
		launch = createLaunch(rp);
		INSTANCES.set(this);
	}

	/**
	 * Finishes a {@link Launch} passed in the method parameters
	 */
	protected void finishLaunch() {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		launch.get().finish(rq);
	}

	/**
	 * Returns a {@link Thread} which is supposed to run on test execution shutdown. By default it finishes the current test execution on
	 * Report Portal.
	 *
	 * @return a thread to run on JVM shutdown event
	 */
	protected Thread getShutdownHook() {
		return new Thread(this::finishLaunch);
	}

	/**
	 * A method for creation a Start Launch request which will be sent to Report Portal. You can customize it by overriding the method.
	 *
	 * @param startTime  launch start time, which will be set into the result request
	 * @param parameters Report Portal client parameters
	 * @return a Start Launch request instance
	 */
	protected StartLaunchRQ buildStartLaunchRQ(Date startTime, ListenerParameters parameters) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(parameters.getLaunchName());
		rq.setStartTime(startTime);
		rq.setMode(parameters.getLaunchRunningMode());
		rq.setAttributes(new HashSet<>(parameters.getAttributes()));
		rq.setDescription(parameters.getDescription());
		rq.setRerun(parameters.isRerun());
		if (isNotBlank(parameters.getRerunOf())) {
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
	 * @param rp a ReportPortal class instance which will be used to communicate with the portal
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
				Runtime.getRuntime().addShutdownHook(getShutdownHook());
				itemTree.setLaunchId(myLaunch.start());
				return myLaunch;
			}
		});
	}

	/**
	 * Creates a {@link StoryReporter} which will be used to report a Story.
	 *
	 * @param factory              JBehave's file print stream factory
	 * @param storyReporterBuilder JBehave's {@link StoryReporter} builder
	 * @return a Story reporter
	 */
	@Override
	public StoryReporter createStoryReporter(FilePrintStreamFactory factory, StoryReporterBuilder storyReporterBuilder) {
		ReportPortalStoryReporter reporter = createReportPortalReporter(factory, storyReporterBuilder);
		STORY_REPORTERS.set(reporter);
		return reporter;
	}

	protected abstract ReportPortalStoryReporter createReportPortalReporter(FilePrintStreamFactory factory,
			StoryReporterBuilder storyReporterBuilder);

	/**
	 * @return a ReportPortal class instance which is used to communicate with the portal
	 */
	@Nonnull
	public ReportPortal getReportPortal() {
		return rp;
	}

	/**
	 * @return a full Test Item Tree with attributes
	 */
	@Nonnull
	public TestItemTree getItemTree() {
		return itemTree;
	}

	/**
	 * Returns a formatter instance for the current thread.
	 *
	 * @return a formatter instance for the current thread
	 */
	@Nonnull
	public static ReportPortalFormat getCurrent() {
		return INSTANCES.get();
	}

	/**
	 * Returns a story reporter for the current thread if any.
	 *
	 * @return a story reporter for the current thread if any
	 */
	@Nonnull
	public static Optional<ReportPortalStoryReporter> getCurrentStoryReporter() {
		return Optional.ofNullable(STORY_REPORTERS.get());
	}
}
