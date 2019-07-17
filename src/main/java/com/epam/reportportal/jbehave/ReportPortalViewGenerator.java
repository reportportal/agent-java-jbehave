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

import org.jbehave.core.model.StoryMaps;
import org.jbehave.core.reporters.ReportsCount;
import org.jbehave.core.reporters.ViewGenerator;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * View Generator to finish launch in ReportPortal
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalViewGenerator implements ViewGenerator {

    private final AtomicBoolean finished;

    public ReportPortalViewGenerator() {
        finished = new AtomicBoolean(false);
    }

    @Override
    public void generateMapsView(File outputDirectory, StoryMaps storyMaps, Properties viewResources) {
        if (finished.compareAndSet(false, true)) {
            JBehaveUtils.finishLaunch();
        }
    }

    @Override
    public void generateReportsView(File outputDirectory, List<String> formats, Properties viewResources) {
        if (finished.compareAndSet(false, true)) {
            JBehaveUtils.finishLaunch();
        }
    }

    @Override
    public ReportsCount getReportsCount() {
        return new ReportsCount(0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public Properties defaultViewProperties() {
        return new Properties();
    }
}
