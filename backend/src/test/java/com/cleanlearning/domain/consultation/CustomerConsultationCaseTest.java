package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;

/**
 * The credits of a case (spec 03, decision 12): without appointments and credit adjustments, the total credits are the
 * purchase terms' session count, and all of them are open.
 */
class CustomerConsultationCaseTest {

    private static CustomerConsultationCase withSessions(int sessionCount) {
        return new CustomerConsultationCase(new ConsultationCaseId(UUID.randomUUID()),
                new OfferingName("Career Coaching"), Optional.empty(),
                new PurchaseTerms(Money.chf("450.00"), new SessionCount(sessionCount), new SessionDuration(60),
                        new CancellationDeadline(24)),
                Instant.parse("2026-11-09T14:03:12.345Z"));
    }

    @Test
    void totalCreditsAreTheSessionCount() {
        assertThat(withSessions(3).totalCredits()).isEqualTo(3);
        assertThat(withSessions(1).totalCredits()).isEqualTo(1);
    }

    @Test
    void everyCreditIsOpen() {
        assertThat(withSessions(3).openCredits()).isEqualTo(3);
        assertThat(withSessions(5).openCredits()).isEqualTo(5);
    }
}
