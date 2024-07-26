package io.jenkins.plugins.nodelabelmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension(ordinal = Integer.MAX_VALUE)
public class ForbiddenLabelListener extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelListener.class.getName());

    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener)
            throws IOException, InterruptedException {
        refreshMonitor();
        LOGGER.log(
                Level.FINE,
                String.format("preOnline() for node '%s' forbidden label. Monitor refreshed", c.getDisplayName()));
        Node node = c.getNode();
        if (node == null) {
            return;
        }
        LOGGER.log(
                Level.FINE,
                String.format(
                        "Node '%s' as assigned labels '%s' is online",
                        node.getDisplayName(), node.getAssignedLabels()));
    }

    /**
     * Triggers the update with 5 seconds quiet period, to avoid triggering data check too often
     * when multiple agents become online at about the same time.
     */
    @Override
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {

        // Mark immediate offline if forbidden label is assigned
        ForbiddenLabelMonitor.DescriptorImpl descriptor =
                NodeMonitor.all().get(ForbiddenLabelMonitor.DescriptorImpl.class);
        LabelAtom labelAtom = descriptor.monitorSynchronous(c);
        if (labelAtom != null) {
            descriptor.markOfflineIfNotIgnored(c, "Node is assigned a forbidden label: " + labelAtom.getDisplayName());
        }
    }

    @Override
    public void onConfigurationChange() {
        refreshMonitor();
        LOGGER.log(Level.FINE, "onConfigurationChange() for forbidden label. Monitor refreshed");
    }

    private void refreshMonitor() {
        NodeMonitor.getAll().stream()
                .filter(nm -> nm instanceof ForbiddenLabelMonitor)
                .map(nm -> (ForbiddenLabelMonitor) nm)
                .forEach(ForbiddenLabelMonitor::waitForUpdate);
    }
}
