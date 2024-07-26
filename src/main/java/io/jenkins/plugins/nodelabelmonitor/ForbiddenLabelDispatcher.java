package io.jenkins.plugins.nodelabelmonitor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.node_monitors.NodeMonitor;
import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class ForbiddenLabelDispatcher extends QueueTaskDispatcher {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelListener.class.getName());

    @Override
    public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {

        Computer c = node.toComputer();
        ForbiddenLabelMonitor.DescriptorImpl descriptor =
                NodeMonitor.all().get(ForbiddenLabelMonitor.DescriptorImpl.class);
        if (descriptor == null) {
            return null;
        }
        if (descriptor.isIgnored()) {
            LOGGER.fine("Forbidden label monitor is ignored. Allowing node to take it");
            return null;
        }
        try {
            LabelAtom labelAtom = descriptor.monitorSynchronous(c);
            if (labelAtom != null) {
                return new ForbiddenLabelBlockage(labelAtom);
            }
            return null;
        } catch (IOException | InterruptedException e) {
            LOGGER.warning("Error while checking forbidden label. Allowing node to take it" + e.getMessage());
            return null;
        }
    }

    public static class ForbiddenLabelBlockage extends CauseOfBlockage {

        private final LabelAtom labelAtom;

        public ForbiddenLabelBlockage(LabelAtom labelAtom) {
            this.labelAtom = labelAtom;
        }

        @Override
        public String getShortDescription() {
            return "Blocked by forbidden label " + labelAtom.getDisplayName();
        }
    }
}
