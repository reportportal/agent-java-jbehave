/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-jbehave
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.jbehave;

import org.jbehave.core.ConfigurableEmbedder;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;

import net.thucydides.jbehave.runners.ThucydidesReportingRunner;

/**
 * Thucydides->JBehave integration
 *
 * @author Andrei Varabyeu
 */
public class RpThucydidesReportingRunner extends ThucydidesReportingRunner {

	public RpThucydidesReportingRunner(Class<? extends ConfigurableEmbedder> testClass) throws Throwable {
		super(testClass);
	}

	public RpThucydidesReportingRunner(Class<? extends ConfigurableEmbedder> testClass, ConfigurableEmbedder embedder) throws Throwable {
		super(testClass, embedder);
	}

	@Override
	protected Configuration getConfiguration() {
		Configuration configuration = super.getConfiguration();
		configuration.useStoryReporterBuilder(new StoryReporterBuilder().withFormats(ReportPortalFormat.INSTANCE));
		return configuration;
	}

	@Test
	@Override
	public void run(RunNotifier notifier) {

		/*
		 * Starts execution in ReportPortal
		 */
		JBehaveUtils.startLaunch();
		super.run(notifier);

		/*
		 * Finishes execution in ReportPortal
		 */
		JBehaveUtils.finishLaunch();
	}

}
