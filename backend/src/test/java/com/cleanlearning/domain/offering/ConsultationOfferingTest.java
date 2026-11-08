package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;

class ConsultationOfferingTest {

    private static final OfferingName AGILE_COACHING = new OfferingName("Agile Coaching");

    private static ConsultationOffering agileCoaching() {
        return ConsultationOffering.create(AGILE_COACHING, "Coaching for agile teams.", new SessionDuration(45),
                new CancellationDeadline(48), List.of());
    }

    private static ConsultationOffering agileCoachingWith(ConsultationPackageEntry... packages) {
        return ConsultationOffering.create(AGILE_COACHING, "Coaching for agile teams.", new SessionDuration(45),
                new CancellationDeadline(48), List.of(packages));
    }

    private static ConsultationOffering edit(ConsultationOffering offering, ConsultationPackageEntry... packages) {
        return offering.edit(AGILE_COACHING, "Coaching for agile teams.", new SessionDuration(45),
                new CancellationDeadline(48), List.of(packages));
    }

    private static ConsultationPackageEntry added(int sessionCount, String price) {
        return ConsultationPackageEntry.added(new SessionCount(sessionCount), Money.chf(price));
    }

    private static ConsultationPackageEntry kept(ConsultationPackage consultationPackage, int sessionCount,
            String price) {
        return ConsultationPackageEntry.kept(consultationPackage.id(), new SessionCount(sessionCount),
                Money.chf(price));
    }

    @Test
    void aNewOfferingIsADraft() {
        ConsultationOffering offering = agileCoaching();

        assertThat(offering.status()).isEqualTo(OfferingStatus.DRAFT);
        assertThat(offering.name()).isEqualTo(AGILE_COACHING);
        assertThat(offering.description()).isEqualTo("Coaching for agile teams.");
        assertThat(offering.sessionDuration()).isEqualTo(new SessionDuration(45));
        assertThat(offering.cancellationDeadline()).isEqualTo(new CancellationDeadline(48));
        assertThat(offering.packages()).isEmpty();
    }

    @Test
    void everyNewOfferingGetsItsOwnId() {
        assertThat(agileCoaching().id()).isNotNull().isNotEqualTo(agileCoaching().id());
    }

    @Test
    void anEditChangesTheValuesAndKeepsIdAndStatus() {
        ConsultationOffering offering = agileCoaching();

        ConsultationOffering edited = offering.edit(new OfferingName("Lean Coaching"), "", new SessionDuration(60),
                new CancellationDeadline(0), List.of());

        assertThat(edited).isEqualTo(new ConsultationOffering(offering.id(), new OfferingName("Lean Coaching"), "",
                new SessionDuration(60), new CancellationDeadline(0), OfferingStatus.DRAFT, List.of()));
    }

    @Test
    void anEditKeepsAPublishedOfferingPublished() {
        ConsultationOffering agileCoaching = agileCoaching();
        ConsultationOffering published = new ConsultationOffering(agileCoaching.id(), agileCoaching.name(),
                agileCoaching.description(), agileCoaching.sessionDuration(), agileCoaching.cancellationDeadline(),
                OfferingStatus.PUBLISHED, List.of());

        assertThat(edit(published).status()).isEqualTo(OfferingStatus.PUBLISHED);
    }

    @Test
    void theDescriptionMayBeEmptyButNotNull() {
        assertThat(ConsultationOffering.create(AGILE_COACHING, "", new SessionDuration(45),
                new CancellationDeadline(48), List.of()).description()).isEmpty();
        assertThatNullPointerException().isThrownBy(() -> ConsultationOffering.create(AGILE_COACHING, null,
                new SessionDuration(45), new CancellationDeadline(48), List.of()));
    }

    @Test
    void aNewOfferingMayHavePackagesAndEachGetsAnId() {
        ConsultationOffering offering = agileCoachingWith(added(1, "160"), added(4, "600"));

        assertThat(offering.packages()).extracting(ConsultationPackage::sessionCount)
                .containsExactly(new SessionCount(1), new SessionCount(4));
        assertThat(offering.packages()).extracting(ConsultationPackage::price)
                .containsExactly(Money.chf("160"), Money.chf("600"));
        assertThat(offering.packages()).extracting(ConsultationPackage::id).doesNotContainNull().doesNotHaveDuplicates();
    }

    @Test
    void aNewOfferingHasNoPackageWithAnId() {
        ConsultationPackageEntry withId = ConsultationPackageEntry.kept(
                new ConsultationPackageId(UUID.randomUUID()), new SessionCount(1), Money.chf("160"));

        assertThatIllegalArgumentException().isThrownBy(() -> agileCoachingWith(withId));
    }

    @Test
    void packagesAreSortedByFewestSessionsThenById() {
        ConsultationPackage three = new ConsultationPackage(id("00000000-0000-0000-0000-000000000003"),
                new SessionCount(3), Money.chf("450"));
        ConsultationPackage oneB = new ConsultationPackage(id("ffffffff-0000-0000-0000-000000000002"),
                new SessionCount(1), Money.chf("100"));
        ConsultationPackage oneA = new ConsultationPackage(id("00000000-0000-0000-0000-000000000001"),
                new SessionCount(1), Money.chf("90"));

        ConsultationOffering offering = new ConsultationOffering(new OfferingId(UUID.randomUUID()), AGILE_COACHING, "",
                new SessionDuration(45), new CancellationDeadline(48), OfferingStatus.DRAFT,
                List.of(three, oneB, oneA));

        // oneB's id starts with ff: as signed numbers it would sort first, as the database sorts a uuid it's last.
        assertThat(offering.packages()).containsExactly(oneA, oneB, three);
    }

    @Test
    void anEditKeepsChangesDropsAndAddsPackagesById() {
        ConsultationOffering offering = agileCoachingWith(added(1, "160"), added(3, "450"), added(5, "700"));
        ConsultationPackage first = offering.packages().get(0);
        ConsultationPackage second = offering.packages().get(1);
        ConsultationPackage third = offering.packages().get(2);

        ConsultationOffering edited = edit(offering, kept(first, 2, "300"), kept(third, 5, "700"),
                added(10, "1000"));

        assertThat(edited.packages()).hasSize(3);
        assertThat(edited.packages().get(0))
                .isEqualTo(new ConsultationPackage(first.id(), new SessionCount(2), Money.chf("300")));
        assertThat(edited.packages().get(1)).isEqualTo(third);
        ConsultationPackage added = edited.packages().get(2);
        assertThat(added.sessionCount()).isEqualTo(new SessionCount(10));
        assertThat(added.price()).isEqualTo(Money.chf("1000"));
        assertThat(added.id()).isNotIn(first.id(), second.id(), third.id());
    }

    @Test
    void anEditWithoutPackagesDropsThemAll() {
        ConsultationOffering offering = agileCoachingWith(added(1, "160"), added(3, "450"));

        assertThat(edit(offering).packages()).isEmpty();
    }

    @Test
    void anEditMayAddTwoPackagesWithTheSameSessionCount() {
        ConsultationOffering edited = edit(agileCoaching(), added(3, "450"), added(3, "400"));

        assertThat(edited.packages()).hasSize(2);
        assertThat(edited.packages()).extracting(ConsultationPackage::sessionCount)
                .containsOnly(new SessionCount(3));
    }

    @Test
    void anEditRejectsAPackageIdOfAnotherOffering() {
        ConsultationOffering offering = agileCoachingWith(added(1, "160"));
        ConsultationOffering other = agileCoachingWith(added(1, "160"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> edit(offering, kept(other.packages().getFirst(), 1, "160")))
                .withMessageContaining(other.packages().getFirst().id().value().toString());
    }

    @Test
    void anEditRejectsAPackageNamedTwice() {
        ConsultationOffering offering = agileCoachingWith(added(1, "160"));
        ConsultationPackage only = offering.packages().getFirst();

        assertThatIllegalArgumentException().isThrownBy(() -> edit(offering, kept(only, 1, "160"), kept(only, 2, "300")));
    }

    private static ConsultationPackageId id(String uuid) {
        return new ConsultationPackageId(UUID.fromString(uuid));
    }
}
