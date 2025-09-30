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

import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jbehave.core.model.Scenario;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * JBehave Reporter for reporting results into ReportPortal. Reports each Story Scenario as a separate test. That means each Scenario
 * has its own statistic and each Scenario Step is reported as nested step and does not add test count.
 *
 * @author Vadzim Hushchanskou
 */
public class ReportPortalScenarioStoryReporter extends ReportPortalStoryReporter {

	public ReportPortalScenarioStoryReporter(final Supplier<Launch> launchSupplier, TestItemTree testItemTree) {
		super(launchSupplier, testItemTree);
	}

	/**
	 * {@inheritDoc}
	 */
	@Nonnull
	protected StartTestItemRQ buildStartExampleRq(@Nonnull Scenario scenario, @Nonnull Map<String, String> example, @Nonnull String codeRef,
			@Nullable final Instant startTime) {
		StartTestItemRQ rq = super.buildStartExampleRq(scenario, example, codeRef, startTime);
		rq.setType(ItemType.STEP.name());
		rq.setTestCaseId(ofNullable(getTestCaseId(codeRef, null)).map(TestCaseIdEntry::getId).orElse(null));
		return rq;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	protected StartTestItemRQ buildStartScenarioRq(@Nonnull Scenario scenario, @Nonnull String codeRef, @Nullable final Instant startTime) {
		StartTestItemRQ rq = super.buildStartScenarioRq(scenario, codeRef, startTime);
		rq.setTestCaseId(ofNullable(getTestCaseId(codeRef, null)).map(TestCaseIdEntry::getId).orElse(null));
		rq.setType(ItemType.STEP.name());
		return rq;
	}

	/**
	 * {@inheritDoc}
	 */
	@Nonnull
	protected StartTestItemRQ buildStartStepRq(@Nonnull final String step, @Nonnull final String codeRef,
			@Nullable final Map<String, String> params, @Nullable final Instant startTime) {
		StartTestItemRQ rq = super.buildStartStepRq(step, codeRef, params, startTime);
		rq.setTestCaseId(null);
		rq.setHasStats(false);
		return rq;
	}
}
