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

import com.epam.reportportal.jbehave.util.ItemTreeUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.StatusEvaluation;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.model.*;
import org.jbehave.core.reporters.NullStoryReporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * JBehave Reporter for reporting results into ReportPortal.
 *
 * @author Vadzim Hushchanskou
 */
public class ReportPortalScenarioStoryReporter extends ReportPortalStepStoryReporter {

	public ReportPortalScenarioStoryReporter(final Supplier<Launch> launchSupplier, TestItemTree testItemTree) {
		super(launchSupplier, testItemTree);
	}

	/**
	 * {@inheritDoc}
	 */
	@Nonnull
	protected StartTestItemRQ buildStartExampleRq(@Nonnull final Map<String, String> example, @Nonnull String codeRef,
			@Nullable final Date startTime) {
		StartTestItemRQ rq = super.buildStartExampleRq(example, codeRef, startTime);
		rq.setType(ItemType.STEP.name());
		rq.setTestCaseId(ofNullable(getTestCaseId(codeRef, null)).map(TestCaseIdEntry::getId).orElse(null));
		return rq;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nonnull
	protected StartTestItemRQ buildStartScenarioRq(@Nonnull Scenario scenario, @Nonnull String codeRef, @Nullable final Date startTime) {
		StartTestItemRQ rq = super.buildStartScenarioRq(scenario, codeRef, startTime);
		if(!scenario.hasExamplesTable() || scenario.getExamplesTable().getRows().isEmpty()) {
			rq.setTestCaseId(ofNullable(getTestCaseId(codeRef, null)).map(TestCaseIdEntry::getId).orElse(null));
			rq.setType(ItemType.STEP.name());
		}
		return rq;
	}

	/**
	 * {@inheritDoc}
	 */
	@Nonnull
	protected StartTestItemRQ buildStartStepRq(@Nonnull final String step, @Nonnull final String codeRef,
			@Nullable final Map<String, String> params, @Nullable final Date startTime) {
		StartTestItemRQ rq = super.buildStartStepRq(step, codeRef, params, startTime);
		rq.setTestCaseId(null);
		rq.setHasStats(false);
		return rq;
	}
}
