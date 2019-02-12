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
