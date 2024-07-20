package io.jenkins.plugins.nodelabelmonitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.MonitorOfflineCause;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class ForbiddenLabelMonitor extends NodeMonitor {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelMonitor.class.getName());

    @DataBoundConstructor
    public ForbiddenLabelMonitor() {}

    @Extension
    @Symbol("forbiddenLabel")
    public static class DescriptorImpl extends AbstractNodeMonitorDescriptor<Boolean> {

        @Override
        protected Boolean monitor(Computer c) throws IOException, InterruptedException {
            Node node = c.getNode();
            if (node == null) {
                return Boolean.FALSE;
            }
            if (node.getDisplayName().equals("Jenkins")) {
                LOGGER.log(Level.FINE, String.format("Ignoring Jenkins Node '%s' for labels", node.getDisplayName()));
                return Boolean.FALSE;
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
                if (!isIgnored()) {
                    markOffline(
                            c,
                            new ForbiddenLabelCause(
                                    "Node is assigned a forbidden label: " + labelAtom.getDisplayName()));
                    LOGGER.log(
                            Level.FINE,
                            String.format(
                                    "Node '%s' is assigned a forbidden label: %s",
                                    node.getDisplayName(), labelAtom.getDisplayName()));
                    return Boolean.TRUE;
                }
            }
            LOGGER.log(Level.FINE, "Reseting Node '%s' to online state", node.getDisplayName());
            c.setTemporarilyOffline(false, null);
            return Boolean.FALSE;
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
