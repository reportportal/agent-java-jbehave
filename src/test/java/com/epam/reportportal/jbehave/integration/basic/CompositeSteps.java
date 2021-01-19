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

package com.epam.reportportal.jbehave.integration.basic;

import org.jbehave.core.annotations.Composite;
import org.jbehave.core.annotations.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeSteps {
	private static final Logger LOGGER = LoggerFactory.getLogger(CompositeSteps.class);

	@Given("composite step")
	@Composite(steps = { "Given I have empty step", "Then I have another empty step" })
	public void compositeStep() {
		LOGGER.info("Inside 'compositeStep' method");
	}

	@Given("failed composite step")
	@Composite(steps = { "Given I have a failed step", "Then I have another empty step" })
	public void failureCompositeStep() {
		LOGGER.info("Inside 'compositeStep' method");
	}
}
