package io.jenkins.plugins.nodelabelmonitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Channel;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ForbiddenLabelListener extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelListener.class.getName());

    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener)
            throws IOException, InterruptedException {
        refreshMonitor();
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

        ForbiddenLabelMonitor.DescriptorImpl descriptor =
                NodeMonitor.all().get(ForbiddenLabelMonitor.DescriptorImpl.class);
        if (descriptor == null) {
            return;
        }
        LabelAtom labelAtom = descriptor.monitorSynchronous(c);

        // If forbidden label is assigned, mark offline or disconnect cloud computer if not monitor is not ignored
        if (labelAtom != null) {

            // Cloud computer
            if (c instanceof AbstractCloudComputer) {
                LOGGER.fine(String.format("Node '%s' is not a cloud computer. Marking offline", c.getName()));
                descriptor.disconnectIfNotIgnored(
                        c,
                        "Node is assigned a forbidden label will disconnect cloud computer: "
                                + labelAtom.getDisplayName());
            }
            // Normal computer
            else {
                LOGGER.fine(String.format("Node '%s' is not a cloud computer", c.getName()));
                descriptor.markOfflineIfNotIgnored(
                        c, "Node is assigned a forbidden label " + labelAtom.getDisplayName());
            }
        }
    }

    @Override
    public void onConfigurationChange() {
        triggerMonitorUpdate();
    }

    @Override
    public void onOffline(@NonNull Computer c, OfflineCause cause) {
        triggerMonitorUpdate();
    }

    @Override
    public void onTemporarilyOffline(Computer c, OfflineCause cause) {
        triggerMonitorUpdate();
    }

    @Override
    public void onTemporarilyOnline(Computer c) {
        triggerMonitorUpdate();
    }

    private void refreshMonitor() {
        NodeMonitor.getAll().stream()
                .filter(nm -> nm instanceof ForbiddenLabelMonitor)
                .map(nm -> (ForbiddenLabelMonitor) nm)
                .forEach(ForbiddenLabelMonitor::waitForUpdate);
    }

    private void triggerMonitorUpdate() {
        NodeMonitor.getAll().stream()
                .filter(nm -> nm instanceof ForbiddenLabelMonitor)
                .map(nm -> (ForbiddenLabelMonitor) nm)
                .forEach(ForbiddenLabelMonitor::triggerUpdate);
    }
}
