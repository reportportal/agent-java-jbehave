/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.jbehave.integration.basic;

import com.epam.reportportal.jbehave.ReportPortalStepFormat;
import com.epam.reportportal.jbehave.ReportPortalStepStoryReporter;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.tree.ItemTreeReporter;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.Given;

import java.util.Calendar;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class CallbackReportingSteps {

	public static final String STEP_TEXT = "I have a step for callback reporting";

	@Given(STEP_TEXT)
	public void a_step_for_callback_reporting() throws InterruptedException {
		Thread.sleep(CommonUtils.MINIMAL_TEST_PAUSE);
	}

	@AfterScenario
	public void after() {
		Optional<ReportPortalStepStoryReporter> reporter = ReportPortalStepFormat.getCurrentStoryReporter();
		ReportPortalStepFormat currentFormat = ReportPortalStepFormat.getCurrent();
		ReportPortal rp = currentFormat.getReportPortal();
		TestItemTree tree = currentFormat.getItemTree();

		reporter.flatMap(ReportPortalStepStoryReporter::getLastStep).ifPresent(itemLeaf -> {
			TestItemTree.TestItemLeaf scenario = itemLeaf.getAttribute(ReportPortalStepStoryReporter.PARENT);
			if (ofNullable(scenario).isPresent()) {
				String scenarioName = ofNullable((StartTestItemRQ) scenario.getAttribute(ReportPortalStepStoryReporter.START_REQUEST)).map(
						StartTestItemRQ::getName).orElseThrow(() -> new IllegalStateException("Unable to get start item request"));
				if (scenarioName.contains("failure")) {
					finishWithStatus(rp, tree, "FAILED", itemLeaf);
					attachLog(rp, tree, itemLeaf);
				} else {
					finishWithStatus(rp, tree, "PASSED", itemLeaf);
				}
			} else {
				throw new IllegalStateException("Unable to find parent item");
			}
		});
	}

	private void finishWithStatus(ReportPortal rp, TestItemTree tree, String status, TestItemTree.TestItemLeaf testItemLeaf) {
		FinishTestItemRQ finishTestItemRQ = new FinishTestItemRQ();
		finishTestItemRQ.setStatus(status);
		finishTestItemRQ.setEndTime(Calendar.getInstance().getTime());
		ItemTreeReporter.finishItem(rp.getClient(), finishTestItemRQ, tree.getLaunchId(), testItemLeaf).cache().blockingGet();
		testItemLeaf.setStatus(ItemStatus.valueOf(status));
	}

	private void attachLog(ReportPortal rp, TestItemTree tree, TestItemTree.TestItemLeaf testItemLeaf) {
		ItemTreeReporter.sendLog(rp.getClient(),
				"ERROR",
				"Error message",
				Calendar.getInstance().getTime(),
				tree.getLaunchId(),
				testItemLeaf
		);
	}

}
