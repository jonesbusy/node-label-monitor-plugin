package io.jenkins.plugins.nodelabelmonitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
public class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule rule) throws Exception {

        // Assert properties
        assertThat(rule.getInstance().getLabelAtom("24.04").getProperties(), emptyIterable());
        assertThat(rule.getInstance().getLabelAtom("Centos-7").getProperties(), hasSize(1));

        // Assert the property
        assertThat(
                rule.getInstance().getLabelAtom("Centos-7").getProperties().get(0),
                instanceOf(ForbiddenLabelProperty.class));
    }
}
