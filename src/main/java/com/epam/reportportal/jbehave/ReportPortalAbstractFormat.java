/*
 * Copyright (C) 2021 EPAM Systems
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
import org.jbehave.core.reporters.Format;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * ReportPortal format. Adds possibility to report story execution results to
 * ReportPortal. Requires using one of execution decorators to start and finish
 * execution in RP
 *
 * @author Vadzim Hushchanskou
 */
public abstract class ReportPortalAbstractFormat extends Format {

	private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	private static final String AGENT_PROPERTIES_FILE = "agent.properties";

	protected final MemoizingSupplier<Launch> launch;
	protected final TestItemTree itemTree = new TestItemTree();
	protected final ReportPortal rp;

	public ReportPortalAbstractFormat(final ReportPortal reportPortal) {
		super("REPORT_PORTAL");
		rp = reportPortal;
		launch = createLaunch(rp);
	}

	protected void finishLaunch(final Launch myLaunch) {
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
				itemTree.setLaunchId(myLaunch.start());
				return myLaunch;
			}
		});
	}

	@Nonnull
	public ReportPortal getReportPortal() {
		return rp;
	}

	@Nonnull
	public TestItemTree getItemTree() {
		return itemTree;
	}
}
