package io.jenkins.plugins.nodelabelmonitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.MonitorOfflineCause;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class ForbiddenLabelMonitor extends NodeMonitor {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelMonitor.class.getName());

    @DataBoundConstructor
    public ForbiddenLabelMonitor() {}

    public void waitForUpdate() {
        LOGGER.log(Level.FINE, "Trigger label monitor update");
        Thread thread = this.triggerUpdate();
        try {
            thread.join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Thread interrupted", e);
        }
        LOGGER.log(Level.FINE, "Label monitor updated");
    }

    @SuppressWarnings("unused") // jelly
    public String toHtml(LabelAtom labelAtom) {
        if (labelAtom == null) {
            return "N/A";
        }
        return Util.wrapToErrorSpan(labelAtom.getDisplayName());
    }

    @Extension(ordinal = Integer.MAX_VALUE)
    @Symbol("forbiddenLabel")
    public static class DescriptorImpl extends AbstractNodeMonitorDescriptor<LabelAtom> {

        /**
         * Mark temporarily offline a node if monitor is not ignored. Typically used for non-cloud computer
         * @param c Computer
         * @param message Message to display
         */
        public void markOfflineIfNotIgnored(Computer c, String message) {
            if (!isIgnored()) {
                markOffline(c, new ForbiddenLabelCause(message));
            }
        }

        /**
         * Disconnect a computer if monitor is not ignored. Typically used for cloud computer
         * @param c Computer
         * @param message Message to display
         */
        public void disconnectIfNotIgnored(Computer c, String message) {
            if (!isIgnored()) {
                try {
                    c.disconnect(new ForbiddenLabelCause(message)).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Error while disconnecting computer", e);
                }
            }
        }

        public @Nullable LabelAtom monitorSynchronous(Computer c) throws IOException, InterruptedException {
            return monitor(c);
        }

        @Override
        protected @Nullable LabelAtom monitor(Computer c) throws IOException, InterruptedException {
            Node node = c.getNode();
            if (node == null) {
                return null;
            }
            if (node.getDisplayName().equals("Jenkins")) {
                LOGGER.log(Level.FINE, String.format("Ignoring Jenkins Node '%s' for labels", node.getDisplayName()));
                return null;
            }
            LOGGER.log(
                    Level.FINE, String.format("Monitoring Node '%s' for forbidden labels...", node.getDisplayName()));
            Set<LabelAtom> assignedLabels = node.getAssignedLabels();
            for (LabelAtom labelAtom : assignedLabels) {
                ForbiddenLabelProperty property = labelAtom.getProperties().get(ForbiddenLabelProperty.class);
                if (property == null) {
                    LOGGER.log(
                            Level.FINE,
                            String.format("Skipping label '%s' for Node '%s'", labelAtom, node.getDisplayName()));
                    continue;
                }
                markOfflineIfNotIgnored(c, "Node is assigned a forbidden label: " + labelAtom.getDisplayName());
                LOGGER.log(
                        Level.FINE,
                        String.format(
                                "Node '%s' is assigned a forbidden label: %s",
                                node.getDisplayName(), labelAtom.getDisplayName()));
                return labelAtom;
            }
            LOGGER.log(Level.FINE, "Reseting Node '%s' to online state", node.getDisplayName());
            c.setTemporarilyOffline(false, null);
            return null;
        }

        @NonNull
        public String getDisplayName() {
            return "Forbidden label";
        }
    }

    public static class ForbiddenLabelCause extends MonitorOfflineCause {

        private final String message;

        public ForbiddenLabelCause(String message) {
            this.message = message;
        }

        @NonNull
        @Override
        public Class<? extends NodeMonitor> getTrigger() {
            return ForbiddenLabelMonitor.class;
        }

        @Override
        @Exported(name = "description")
        public String toString() {
            return message;
        }
    }
}
