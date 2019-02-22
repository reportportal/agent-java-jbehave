/*
 * Copyright (C) 2019 EPAM Systems
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

import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.jbehave.core.model.ExamplesTable;
import org.junit.Test;

/**
 * @author Valery Yatsynovich
 */
public class JBehaveContextStoryTest {

    @Test
    public void testStoryHasNoExamplesWhenExamplesTableIsEmpty() {
        JBehaveContext.Story story = new JBehaveContext.Story();
        story.setExamples(new JBehaveContext.Examples(Collections.<String>emptyList(), ExamplesTable.EMPTY));
        assertFalse(story.hasExamples());
    }
}
