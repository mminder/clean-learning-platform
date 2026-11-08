package com.cleanlearning.domain.offering.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class OfferingNameTest {

    @Test
    void holdsTheName() {
        assertThat(new OfferingName("Career Coaching").value()).isEqualTo("Career Coaching");
    }

    @Test
    void rejectsEmptyAndBlankNames() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OfferingName(""));
        assertThatIllegalArgumentException().isThrownBy(() -> new OfferingName(" \t\n"));
    }

    @Test
    void isRequired() {
        assertThatNullPointerException().isThrownBy(() -> new OfferingName(null));
    }

    @Test
    void isComparedByValue() {
        assertThat(new OfferingName("Career Coaching")).isEqualTo(new OfferingName("Career Coaching"));
        assertThat(new OfferingName("Career Coaching")).isNotEqualTo(new OfferingName("Software Architecture"));
    }
}
