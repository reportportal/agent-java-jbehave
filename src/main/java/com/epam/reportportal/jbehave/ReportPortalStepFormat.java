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

import com.epam.reportportal.service.ReportPortal;
import org.jbehave.core.reporters.FilePrintStreamFactory;
import org.jbehave.core.reporters.StoryReporterBuilder;

/**
 * A format to report JBehave runs into Report Portal application. Each Scenario Step reported with the format will have its own statistics.
 * Scenarios will not have it.
 *
 * @author Vadzim Hushchanskou
 */
public class ReportPortalStepFormat extends ReportPortalFormat {
	public static final ReportPortalStepFormat INSTANCE = new ReportPortalStepFormat();

	public ReportPortalStepFormat() {
		this(ReportPortal.builder().build());
	}

	public ReportPortalStepFormat(final ReportPortal reportPortal) {
		super(reportPortal);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReportPortalStoryReporter createReportPortalReporter(FilePrintStreamFactory factory, StoryReporterBuilder storyReporterBuilder) {
		return new ReportPortalStepStoryReporter(launch, itemTree);
	}
}
