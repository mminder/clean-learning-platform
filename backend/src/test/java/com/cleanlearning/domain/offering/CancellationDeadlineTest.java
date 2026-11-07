package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class CancellationDeadlineTest {

    @Test
    void isANumberOfHours() {
        assertThat(new CancellationDeadline(24).hours()).isEqualTo(24);
    }

    @Test
    void acceptsZeroHours() {
        // No deadline: cancelling is free until the session starts.
        assertThat(new CancellationDeadline(0).hours()).isZero();
    }

    @Test
    void rejectsNegativeHours() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CancellationDeadline(-1)).withMessageContaining("-1");
    }

    @Test
    void isComparedByValue() {
        assertThat(new CancellationDeadline(24)).isEqualTo(new CancellationDeadline(24));
        assertThat(new CancellationDeadline(24)).isNotEqualTo(new CancellationDeadline(48));
    }
}
