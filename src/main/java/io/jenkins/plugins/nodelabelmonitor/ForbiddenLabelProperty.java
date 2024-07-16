package io.jenkins.plugins.nodelabelmonitor;

import hudson.Extension;
import hudson.model.labels.LabelAtomProperty;
import hudson.model.labels.LabelAtomPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class ForbiddenLabelProperty extends LabelAtomProperty {

    @DataBoundConstructor
    public ForbiddenLabelProperty() {
    }

    @Extension
    public static class DescriptorImpl extends LabelAtomPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "Forbidden Label";
        }

    }

}
