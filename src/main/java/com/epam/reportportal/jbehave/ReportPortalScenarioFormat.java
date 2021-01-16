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
import org.jbehave.core.reporters.StoryReporter;
import org.jbehave.core.reporters.StoryReporterBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * ReportPortal format. Adds possibility to report story execution results to
 * ReportPortal. Requires using one of execution decorators to start and finish
 * execution in RP
 *
 * @author Vadzim Hushchanskou
 */
public class ReportPortalScenarioFormat extends AbstractReportPortalFormat {
	private static final ThreadLocal<ReportPortalScenarioFormat> INSTANCES = new InheritableThreadLocal<>();
	private static final ThreadLocal<ReportPortalScenarioStoryReporter> STORY_REPORTERS = new InheritableThreadLocal<>();
	public static final ReportPortalScenarioFormat INSTANCE = new ReportPortalScenarioFormat();

	public ReportPortalScenarioFormat() {
		this(ReportPortal.builder().build());
	}

	public ReportPortalScenarioFormat(final ReportPortal reportPortal) {
		super(reportPortal);
		INSTANCES.set(this);
	}

	@Override
	public StoryReporter createStoryReporter(FilePrintStreamFactory factory, StoryReporterBuilder storyReporterBuilder) {
		ReportPortalScenarioStoryReporter reporter = new ReportPortalScenarioStoryReporter(launch, itemTree);
		STORY_REPORTERS.set(reporter);
		return reporter;
	}

	@Nonnull
	public static ReportPortalScenarioFormat getCurrent() {
		return INSTANCES.get();
	}

	@Nonnull
	public static Optional<ReportPortalScenarioStoryReporter> getCurrentStoryReporter() {
		return Optional.ofNullable(STORY_REPORTERS.get());
	}
}
