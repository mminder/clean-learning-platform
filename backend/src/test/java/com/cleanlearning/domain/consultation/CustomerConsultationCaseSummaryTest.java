package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;

/**
 * The credits of a case on the list: the same as {@link CustomerConsultationCase} computes them (spec 03,
 * decision 12).
 */
class CustomerConsultationCaseSummaryTest {

    private static CustomerConsultationCaseSummary withSessions(int sessionCount) {
        return new CustomerConsultationCaseSummary(new ConsultationCaseId(UUID.randomUUID()),
                new OfferingName("Career Coaching"), Optional.empty(), new SessionCount(sessionCount),
                Instant.parse("2026-11-09T14:03:12.345Z"));
    }

    @Test
    void totalCreditsAreTheSessionCount() {
        assertThat(withSessions(3).totalCredits()).isEqualTo(3);
        assertThat(withSessions(5).totalCredits()).isEqualTo(5);
    }

    @Test
    void everyCreditIsOpen() {
        assertThat(withSessions(3).openCredits()).isEqualTo(3);
        assertThat(withSessions(1).openCredits()).isEqualTo(1);
    }
}
