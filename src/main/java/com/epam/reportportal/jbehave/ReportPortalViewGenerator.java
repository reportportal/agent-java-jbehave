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
