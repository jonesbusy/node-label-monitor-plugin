package io.jenkins.plugins.nodelabelmonitor;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.node_monitors.NodeMonitor;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension(ordinal = Integer.MIN_VALUE)
public class ForbiddenLabelListener extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelListener.class.getName());

    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener)
            throws IOException, InterruptedException {
        refreshMonitor();
        LOGGER.log(Level.FINE, "preOnline() for forbidden label. Monitor refreshed");
    }

    @Override
    public void onConfigurationChange() {
        refreshMonitor();
        LOGGER.log(Level.FINE, "onConfigurationChange() for forbidden label. Monitor refreshed");
    }

    private void refreshMonitor() {
        NodeMonitor.getAll().stream()
                .filter(nm -> nm instanceof ForbiddenLabelMonitor)
                .forEach(nm -> {
                    try {
                        Thread thread = nm.triggerUpdate();
                        thread.join();
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Error refreshing monitor", e);
                    }
                });
    }
}
