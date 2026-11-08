package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.Expert;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;

/**
 * The status table of UC-16 (the plan's errors section in _docs/tasks/02-manage-consultation-offerings/README.md),
 * cell by cell: edit, publish, unpublish, archive and delete in each status.
 */
class ConsultationOfferingStatusTest {

    private static final Expert EXPERT_1 = new Expert(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000201")), new DisplayName("Expert 1"));

    private static final ConsultationPackage ONE_SESSION = new ConsultationPackage(
            new ConsultationPackageId(UUID.randomUUID()), new SessionCount(1), Money.chf("160"));

    private static final String ARCHIVED_MESSAGE = "The offering is archived and can't be changed any more.";

    /** An offering in this status with a package and an eligible expert. */
    private static ConsultationOffering complete(OfferingStatus status) {
        return offering(status, List.of(ONE_SESSION), Set.of(EXPERT_1.id()));
    }

    private static ConsultationOffering offering(OfferingStatus status, List<ConsultationPackage> packages,
            Set<UserId> eligibleExpertIds) {
        return new ConsultationOffering(new OfferingId(UUID.randomUUID()), new OfferingName("Agile Coaching"),
                "Coaching for agile teams.", new SessionDuration(45), new CancellationDeadline(48), status, packages,
                eligibleExpertIds);
    }

    private static ConsultationOffering edit(ConsultationOffering offering) {
        return offering.edit(new OfferingName("Lean Coaching"), "", new SessionDuration(60),
                new CancellationDeadline(24), List.of(), Set.of(), List.of(EXPERT_1));
    }

    private static BusinessRuleException refusal(ConsultationOffering offering,
            Consumer<ConsultationOffering> change) {
        BusinessRuleException exception = catchThrowableOfType(BusinessRuleException.class,
                () -> change.accept(offering));
        assertThat(exception).as("refused").isNotNull();
        return exception;
    }

    private static void assertWrongStatus(ConsultationOffering offering, Consumer<ConsultationOffering> change,
            String message) {
        BusinessRuleException exception = refusal(offering, change);
        assertThat(exception.code()).isEqualTo("WRONG_OFFERING_STATUS");
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    // DRAFT

    @Test
    void aDraftMayBeEdited() {
        assertThat(edit(complete(OfferingStatus.DRAFT)).status()).isEqualTo(OfferingStatus.DRAFT);
    }

    @Test
    void aCompleteDraftIsPublished() {
        ConsultationOffering draft = complete(OfferingStatus.DRAFT);

        ConsultationOffering published = draft.publish();

        assertThat(published).isEqualTo(new ConsultationOffering(draft.id(), draft.name(), draft.description(),
                draft.sessionDuration(), draft.cancellationDeadline(), OfferingStatus.PUBLISHED, draft.packages(),
                draft.eligibleExpertIds()));
        assertThat(draft.status()).as("unchanged").isEqualTo(OfferingStatus.DRAFT);
    }

    @Test
    void publishingADraftWithoutAPackageNamesTheMissingPackage() {
        BusinessRuleException exception = refusal(offering(OfferingStatus.DRAFT, List.of(), Set.of(EXPERT_1.id())),
                ConsultationOffering::publish);

        assertThat(exception.code()).isEqualTo("OFFERING_INCOMPLETE");
        assertThat(exception.getMessage())
                .isEqualTo("The offering needs at least one package before it can be published.");
    }

    @Test
    void publishingADraftWithoutAnEligibleExpertNamesTheMissingExpert() {
        BusinessRuleException exception = refusal(offering(OfferingStatus.DRAFT, List.of(ONE_SESSION), Set.of()),
                ConsultationOffering::publish);

        assertThat(exception.code()).isEqualTo("OFFERING_INCOMPLETE");
        assertThat(exception.getMessage())
                .isEqualTo("The offering needs at least one eligible expert before it can be published.");
    }

    @Test
    void publishingADraftWithNeitherNamesBoth() {
        BusinessRuleException exception = refusal(offering(OfferingStatus.DRAFT, List.of(), Set.of()),
                ConsultationOffering::publish);

        assertThat(exception.code()).isEqualTo("OFFERING_INCOMPLETE");
        assertThat(exception.getMessage()).isEqualTo(
                "The offering needs at least one package and at least one eligible expert before it can be published.");
    }

    @Test
    void aDraftCantBeUnpublished() {
        assertWrongStatus(complete(OfferingStatus.DRAFT), ConsultationOffering::unpublish,
                "The offering is a draft. Only a published offering can be unpublished.");
    }

    @Test
    void aDraftCantBeArchived() {
        assertWrongStatus(complete(OfferingStatus.DRAFT), ConsultationOffering::archive,
                "The offering is a draft. Only a published offering can be archived.");
    }

    @Test
    void aDraftMayBeDeleted() {
        assertThatCode(complete(OfferingStatus.DRAFT)::requireDeletable).doesNotThrowAnyException();
        assertThatCode(offering(OfferingStatus.DRAFT, List.of(), Set.of())::requireDeletable)
                .doesNotThrowAnyException();
    }

    // PUBLISHED

    @Test
    void aPublishedOfferingMayBeEdited() {
        assertThat(edit(complete(OfferingStatus.PUBLISHED)).status()).isEqualTo(OfferingStatus.PUBLISHED);
    }

    @Test
    void aPublishedOfferingCantBePublishedAgain() {
        assertWrongStatus(complete(OfferingStatus.PUBLISHED), ConsultationOffering::publish,
                "The offering is published. Only a draft can be published.");
    }

    @Test
    void aPublishedOfferingIsUnpublishedToADraft() {
        ConsultationOffering published = complete(OfferingStatus.PUBLISHED);

        ConsultationOffering unpublished = published.unpublish();

        assertThat(unpublished.status()).isEqualTo(OfferingStatus.DRAFT);
        assertThat(unpublished.id()).isEqualTo(published.id());
        assertThat(unpublished.packages()).isEqualTo(published.packages());
        assertThat(unpublished.eligibleExpertIds()).isEqualTo(published.eligibleExpertIds());
    }

    @Test
    void anIncompletePublishedOfferingMayBeUnpublished() {
        // UC-16 lets an admin remove every package or expert of a published offering.
        assertThat(offering(OfferingStatus.PUBLISHED, List.of(), Set.of()).unpublish().status())
                .isEqualTo(OfferingStatus.DRAFT);
    }

    @Test
    void aPublishedOfferingIsArchived() {
        ConsultationOffering published = complete(OfferingStatus.PUBLISHED);

        ConsultationOffering archived = published.archive();

        assertThat(archived.status()).isEqualTo(OfferingStatus.ARCHIVED);
        assertThat(archived.id()).isEqualTo(published.id());
        assertThat(archived.packages()).isEqualTo(published.packages());
    }

    @Test
    void aPublishedOfferingCantBeDeleted() {
        assertWrongStatus(complete(OfferingStatus.PUBLISHED), ConsultationOffering::requireDeletable,
                "The offering is published. Only a draft can be deleted.");
    }

    // ARCHIVED: final and read-only

    @Test
    void anArchivedOfferingCantBeEdited() {
        assertWrongStatus(complete(OfferingStatus.ARCHIVED), ConsultationOfferingStatusTest::edit, ARCHIVED_MESSAGE);
    }

    @Test
    void anArchivedOfferingCantBePublished() {
        assertWrongStatus(complete(OfferingStatus.ARCHIVED), ConsultationOffering::publish, ARCHIVED_MESSAGE);
    }

    @Test
    void anIncompleteArchivedOfferingCantBePublishedEither() {
        // the status comes first: what's missing doesn't matter for an archived offering
        assertWrongStatus(offering(OfferingStatus.ARCHIVED, List.of(), Set.of()), ConsultationOffering::publish,
                ARCHIVED_MESSAGE);
    }

    @Test
    void anArchivedOfferingCantBeUnpublished() {
        assertWrongStatus(complete(OfferingStatus.ARCHIVED), ConsultationOffering::unpublish, ARCHIVED_MESSAGE);
    }

    @Test
    void anArchivedOfferingCantBeArchivedAgain() {
        assertWrongStatus(complete(OfferingStatus.ARCHIVED), ConsultationOffering::archive, ARCHIVED_MESSAGE);
    }

    @Test
    void anArchivedOfferingCantBeDeleted() {
        assertWrongStatus(complete(OfferingStatus.ARCHIVED), ConsultationOffering::requireDeletable,
                ARCHIVED_MESSAGE);
    }
}
