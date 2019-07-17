/*
 * Copyright 2019 EPAM Systems
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

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.StoryReporterBuilder;

/**
 * Wraps {@link JUnitStories} to be able to start and finish execution in
 * ReportPortal
 *
 * @author Andrei Varabyeu
 */
public abstract class RpJUnitStories extends JUnitStories {

    /**
     * Adds ReportPortalFormat to be able to report results to ReportPortal
     */
    @Override
    public Configuration configuration() {
        return super.configuration()
                .useStoryReporterBuilder(new StoryReporterBuilder().withFormats(ReportPortalFormat.INSTANCE))
                .useViewGenerator(new ReportPortalViewGenerator());
    }
}
