package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionDuration;

class ConsultationOfferingTest {

    private static ConsultationOffering agileCoaching() {
        return ConsultationOffering.create(new OfferingName("Agile Coaching"), "Coaching for agile teams.",
                new SessionDuration(45), new CancellationDeadline(48));
    }

    @Test
    void aNewOfferingIsADraft() {
        ConsultationOffering offering = agileCoaching();

        assertThat(offering.status()).isEqualTo(OfferingStatus.DRAFT);
        assertThat(offering.name()).isEqualTo(new OfferingName("Agile Coaching"));
        assertThat(offering.description()).isEqualTo("Coaching for agile teams.");
        assertThat(offering.sessionDuration()).isEqualTo(new SessionDuration(45));
        assertThat(offering.cancellationDeadline()).isEqualTo(new CancellationDeadline(48));
    }

    @Test
    void everyNewOfferingGetsItsOwnId() {
        assertThat(agileCoaching().id()).isNotNull().isNotEqualTo(agileCoaching().id());
    }

    @Test
    void anEditChangesTheValuesAndKeepsIdAndStatus() {
        ConsultationOffering offering = agileCoaching();

        ConsultationOffering edited = offering.edit(new OfferingName("Lean Coaching"), "", new SessionDuration(60),
                new CancellationDeadline(0));

        assertThat(edited).isEqualTo(new ConsultationOffering(offering.id(), new OfferingName("Lean Coaching"), "",
                new SessionDuration(60), new CancellationDeadline(0), OfferingStatus.DRAFT));
    }

    @Test
    void anEditKeepsAPublishedOfferingPublished() {
        ConsultationOffering agileCoaching = agileCoaching();
        ConsultationOffering published = new ConsultationOffering(agileCoaching.id(), agileCoaching.name(),
                agileCoaching.description(), agileCoaching.sessionDuration(), agileCoaching.cancellationDeadline(),
                OfferingStatus.PUBLISHED);

        assertThat(published.edit(new OfferingName("Lean Coaching"), "", new SessionDuration(60),
                new CancellationDeadline(0)).status()).isEqualTo(OfferingStatus.PUBLISHED);
    }

    @Test
    void theDescriptionMayBeEmptyButNotNull() {
        assertThat(ConsultationOffering.create(new OfferingName("Agile Coaching"), "", new SessionDuration(45),
                new CancellationDeadline(48)).description()).isEmpty();
        assertThatNullPointerException().isThrownBy(() -> ConsultationOffering.create(
                new OfferingName("Agile Coaching"), null, new SessionDuration(45), new CancellationDeadline(48)));
    }
}
