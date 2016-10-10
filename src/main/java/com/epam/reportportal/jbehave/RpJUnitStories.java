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

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.junit.Test;

import com.epam.reportportal.listeners.Statuses;

/**
 * Wraps {@link JUnitStories} to be able to start and finish execution in
 * ReportPortal
 *
 * @author Andrei Varabyeu
 */
public abstract class RpJUnitStories extends JUnitStories {

	@Test
	@Override
	public void run() throws Throwable {
		/*
		 * Starts execution in ReportPortal
		 */
		JBehaveUtils.startLaunch();
		try {
			super.run();
		} finally {
			JBehaveUtils.makeSureAllItemsFinished(Statuses.FAILED);
			/*
			 * Finishes execution in ReportPortal
			 */
			JBehaveUtils.finishLaunch();
		}

	}

	/**
	 * Adds ReportPortalFormat to be able to report results to ReportPortal
	 */
	@Override
	public Configuration configuration() {
		return super.configuration().useStoryReporterBuilder(new StoryReporterBuilder().withFormats(ReportPortalFormat.INSTANCE));
	}
}
