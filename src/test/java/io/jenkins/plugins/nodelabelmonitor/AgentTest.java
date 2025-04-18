package io.jenkins.plugins.nodelabelmonitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.slaves.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class AgentTest {

    private static class MockCloudAgent extends AbstractCloudSlave {

        protected MockCloudAgent(@NotNull String name, String remoteFS, ComputerLauncher launcher)
                throws Descriptor.FormException, IOException {
            super(name, remoteFS, launcher);
        }

        @Override
        public MockCloudComputer createComputer() {
            return new MockCloudComputer(this);
        }

        @Override
        protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
            return;
        }
    }

    private static class MockCloudComputer extends AbstractCloudComputer<MockCloudAgent> {

        public MockCloudComputer(MockCloudAgent slave) {
            super(slave);
        }
    }

    @Rule
    public LoggerRule logs = new LoggerRule()
            .record("io.jenkins.plugins.nodelabelmonitor", Level.FINEST)
            .record("hudson.node_monitors", Level.FINEST)
            .record("org.jvnet.hudson.plugins.platformlabeler", Level.FINEST);

    @Test
    public void shouldAcceptTasks(JenkinsRule rule) throws Exception {
        rule.createOnlineSlave(Label.get("foobar"));

        // Computer should be online
        assertThat(rule.getInstance().getComputer("slave0").isOnline(), equalTo(true));
    }

    @Test
    public void shouldAcceptTasksForCloudAgent(JenkinsRule rule) throws Exception {
        createCloudAgent("cloud0", Label.get("foobar"), rule, true);

        // Computer should be online
        assertThat(rule.getInstance().getComputer("cloud0").isOnline(), equalTo(true));
    }

    @Test
    public void shouldBeOfflineWithLabel(JenkinsRule rule) throws Exception {
        // Create a new forbidden label
        rule.getInstance().getLabelAtom("barfoo").getProperties().add(new ForbiddenLabelProperty());
        assertThat(rule.getInstance().getLabelAtom("barfoo").getProperties().size(), equalTo(1));
        rule.createOnlineSlave(Label.get("barfoo"));

        // Computer should be offline
        Computer computer = rule.getInstance().getComputer("slave0");
        assertThat(computer.isOffline(), equalTo(true));
        assertThat(computer.getOfflineCause(), instanceOf(ForbiddenLabelMonitor.ForbiddenLabelCause.class));
    }

    @Test
    public void shouldBeOfflineWithLabelAndCloudAgent(JenkinsRule rule) throws Exception {
        // Create a new forbidden label
        rule.getInstance().getLabelAtom("barfoo").getProperties().add(new ForbiddenLabelProperty());
        assertThat(rule.getInstance().getLabelAtom("barfoo").getProperties().size(), equalTo(1));
        createCloudAgent("cloud0", Label.get("barfoo"), rule, false);

        // Wait a bit because the agent should be disconnected with a cause (will never gets online)
        Thread.sleep(1000);

        // Computer should be offline
        Computer computer = rule.getInstance().getComputer("cloud0");
        assertThat(computer.isOffline(), equalTo(true));
        assertThat(computer.getOfflineCause(), instanceOf(ForbiddenLabelMonitor.ForbiddenLabelCause.class));
    }

    @Test
    public void shouldBeOfflineWithPlatformLabel(JenkinsRule rule) throws Exception {
        // Create a new forbidden label
        rule.getInstance().getLabelAtom("Linux").getProperties().add(new ForbiddenLabelProperty());
        rule.getInstance().getLabelAtom("Windows").getProperties().add(new ForbiddenLabelProperty());
        rule.createOnlineSlave(); // Don't assign any label

        // Computer should be offline
        Computer computer = rule.getInstance().getComputer("slave0");
        assertThat(computer.isOffline(), equalTo(true));
        assertThat(computer.getOfflineCause(), instanceOf(ForbiddenLabelMonitor.ForbiddenLabelCause.class));
    }

    private MockCloudAgent createCloudAgent(String nodeName, Label label, JenkinsRule rule, boolean waitOnline)
            throws Exception {
        MockCloudAgent slave = new MockCloudAgent(
                nodeName,
                (new File(rule.getInstance().getRootDir(), "agent-work-dirs/" + nodeName)).getAbsolutePath(),
                rule.createComputerLauncher(null));
        slave.setRetentionStrategy(RetentionStrategy.NOOP);
        slave.setLabelString(label.getName());
        rule.getInstance().addNode(slave);
        if (waitOnline) {
            rule.waitOnline(slave);
        }
        return slave;
    }
}
