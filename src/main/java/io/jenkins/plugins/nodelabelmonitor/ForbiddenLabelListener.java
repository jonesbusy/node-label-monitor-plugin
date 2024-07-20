package io.jenkins.plugins.nodelabelmonitor;

import hudson.slaves.ComputerListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForbiddenLabelListener extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(ForbiddenLabelListener.class.getName());

    @Override
    public void onConfigurationChange() {
        LOGGER.log(Level.FINE, "onConfigurationChange() for forbidden label. Refreshing monitor");
    }
}
