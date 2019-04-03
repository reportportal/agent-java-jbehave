package com.epam.reportportal.jbehave;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.reportportal.jbehave.JBehaveContext.Story;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortal.Builder;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.reactivex.Maybe;

@RunWith(PowerMockRunner.class)
public class ReportPortalStoryReporterTests
{
    private static final String STEP = "step";
    
    @PrepareForTest({ ReportPortal.class, JBehaveUtils.class })
    @Test
    public void testResetTestStatus() throws Exception {
        PowerMockito.spy(JBehaveUtils.class);
        Story story = JBehaveContext.getCurrentStory();
        Maybe<String> scenario = Maybe.just("scenario");
        Maybe<String> step = Maybe.just(STEP);
        story.setCurrentScenario(scenario);
        Launch launch = mockLaunch();
        when(launch.startTestItem(eq(scenario), any(StartTestItemRQ.class))).thenReturn(step);
        ReportPortalStoryReporter storyReporter = new ReportPortalStoryReporter();
        storyReporter.beforeStep(STEP);
        verifyStep(story, step, Statuses.PASSED);

        Exception exception = mock(Exception.class);
        PowerMockito.doNothing().when(JBehaveUtils.class, "sendStackTraceToRP", exception);
        storyReporter.failed(STEP, exception);
        verify(launch).finishTestItem(eq(step), argThat(new ArgumentMatcher<FinishTestItemRQ>() {
            @Override
            public boolean matches(FinishTestItemRQ argument) {
                return Statuses.FAILED.equals(argument.getStatus());
            }
        }));
        story.setCurrentStepStatus(Statuses.FAILED);
        storyReporter.beforeStep(STEP);
        verifyStep(story, step, Statuses.PASSED);
    }

    private static void verifyStep(Story story, Maybe<String> step, String status) {
        Assert.assertEquals(step, story.getCurrentStep());
        Assert.assertEquals(Statuses.PASSED, story.getCurrentStepStatus());
    }

    private static ListenerParameters createListenerParameters() {
        ListenerParameters parameters = new ListenerParameters();
        parameters.setLaunchName("launchName");
        parameters.setLaunchRunningMode(Mode.DEFAULT);
        parameters.setTags(Collections.<String>emptySet());
        parameters.setDescription("description");
        return parameters;
    }

    private static Launch mockLaunch() throws Exception {
        PowerMockito.mockStatic(ReportPortal.class);
        Builder builder = mock(Builder.class);
        Launch launch = mock(Launch.class);
        ReportPortal reportPortal = mock(ReportPortal.class);
        PowerMockito.when(ReportPortal.class, "builder").thenReturn(builder);
        when(builder.build()).thenReturn(reportPortal);
        when(reportPortal.getParameters()).thenReturn(createListenerParameters());
        when(reportPortal.newLaunch(any(StartLaunchRQ.class))).thenReturn(launch);
        return launch;
    }
}
