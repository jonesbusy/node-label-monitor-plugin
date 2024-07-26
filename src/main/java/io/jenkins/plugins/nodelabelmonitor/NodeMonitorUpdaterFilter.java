package io.jenkins.plugins.nodelabelmonitor;

import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.node_monitors.NodeMonitorUpdater;
import java.util.logging.Logger;
import jenkins.ExtensionFilter;

@Extension
public class NodeMonitorUpdaterFilter extends ExtensionFilter {

    public static final Logger LOGGER = Logger.getLogger(NodeMonitorUpdaterFilter.class.getName());

    @Override
    public <T> boolean allows(Class<T> tClass, ExtensionComponent<T> tExtensionComponent) {
        if (tExtensionComponent.getInstance().getClass() == NodeMonitorUpdater.class) {
            LOGGER.fine("NodeMonitorUpdater is disabled and replaced by plugin one");
            return false;
        }
        return true;
    }
}
