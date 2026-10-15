package dev.yaadt.controller.beads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class BeadsProjectLabelsTest {
    @Test
    void derivesAnExactProjectScopedLabel() {
        assertThat(BeadsProjectLabels.forProject(" beads-test ")).isEqualTo("project:beads-test");
        assertThat(BeadsProjectLabels.forProject("yaadt")).isNotEqualTo("project:beads-test");
    }

    @Test
    void rejectsBlankProjectNames() {
        assertThatIllegalArgumentException().isThrownBy(() -> BeadsProjectLabels.forProject(" "));
    }
}
