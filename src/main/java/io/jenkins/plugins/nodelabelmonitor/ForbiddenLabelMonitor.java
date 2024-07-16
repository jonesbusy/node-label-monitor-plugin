package io.jenkins.plugins.nodelabelmonitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.MonitorOfflineCause;
import hudson.node_monitors.NodeMonitor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForbiddenLabelMonitor extends NodeMonitor {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelMonitor.class.getName());

    @DataBoundConstructor
    public ForbiddenLabelMonitor() {
    }

    @Extension
    @Symbol("forbiddenLabel")
    public static class DescriptorImpl extends AbstractNodeMonitorDescriptor<Boolean> {

        @Override
        protected Boolean monitor(Computer c) throws IOException, InterruptedException {
            Node node = c.getNode();
            if (node == null) {
                return Boolean.FALSE;
            }
            LOGGER.log(Level.FINE, "Monitoring Node '{0}' for forbidden labels...", new Object[]{node.getDisplayName()});
            Set<LabelAtom> assignedLabels = node.getAssignedLabels();
            for(LabelAtom labelAtom : assignedLabels) {
                ForbiddenLabelProperty property = labelAtom.getProperties().get(ForbiddenLabelProperty.class);
                if (property == null) {
                    LOGGER.log(Level.FINE, "Skipping label '{0}' for Node '{1}'", new Object[]{labelAtom, node.getDisplayName()});
                    continue;
                }
                if (!isIgnored()) {
                    markOffline(c, new ForbiddenLabelCause("Node is assigned a forbidden label: " + labelAtom.getDisplayName()));
                    LOGGER.log(Level.FINE, "Node '{0}' is assigned a forbidden label: '{1}'", new Object[]{node.getDisplayName(), labelAtom.getDisplayName()});
                    return Boolean.TRUE;
                }
            }
            LOGGER.log(Level.FINE, "Reseting Node '{0}' to online state", new Object[]{node.getDisplayName()});
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
    }

}
