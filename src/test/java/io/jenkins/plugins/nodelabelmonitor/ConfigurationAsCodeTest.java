package io.jenkins.plugins.nodelabelmonitor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import hudson.model.labels.LabelAtom;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
public class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code(JenkinsConfiguredWithCodeRule rule) throws Exception {
        List<LabelAtom> atoms = new ArrayList<>(rule.getInstance().getLabelAtoms());
        assertThat(atoms.size(), equalTo(5));
        assertThat(atoms, hasItems(equalTo(LabelAtom.get("24.04")), equalTo(LabelAtom.get("Centos-7"))));

        // Assert properties
        assertThat(rule.getInstance().getLabelAtom("24.04").getProperties(), emptyIterable());
        assertThat(rule.getInstance().getLabelAtom("Centos-7").getProperties(), hasSize(1));

        // Assert the property
        assertThat(
                rule.getInstance().getLabelAtom("Centos-7").getProperties().get(0),
                instanceOf(ForbiddenLabelProperty.class));
    }
}
