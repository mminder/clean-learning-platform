package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.outcome.Booking;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.BookableSlot;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.user.ExpertProfile;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** {@link #book} re-checks every rule itself, the status first. */
public record CaseForBooking(
    ConsultationCaseId id,
    CaseStatus status,
    PurchaseTerms purchaseTerms,
    Optional<UserId> assignedExpertId,
    CaseCredits credits,
    List<ExpertProfile> eligibleExperts) {

  public static final String NO_OPEN_CREDITS = "NO_OPEN_CREDITS";

  public static final String EXPERT_CHANGED = "EXPERT_CHANGED";

  public static final String EXPERT_NOT_ELIGIBLE = "EXPERT_NOT_ELIGIBLE";

  public static final String SLOT_NOT_AVAILABLE = "SLOT_NOT_AVAILABLE";

  public CaseForBooking {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    Objects.requireNonNull(assignedExpertId, "assignedExpertId");
    Objects.requireNonNull(credits, "credits");
    credits.requireSessionCountOf(purchaseTerms);
    eligibleExperts = List.copyOf(eligibleExperts);
  }

  public void requireActive() {
    if (status == CaseStatus.CANCELLED) {
      throw new BusinessRuleException(
          CaseForCancellation.CASE_CANCELLED, CaseForCancellation.CASE_CANCELLED_DETAIL);
    }
  }

  public void requireOpenCredit() {
    if (!credits.hasOpenCredit()) {
      throw new BusinessRuleException(NO_OPEN_CREDITS, "This case has no open credits.");
    }
  }

  /**
   * The case's expert stays after they stop being eligible; only a first booking needs an eligible
   * one.
   */
  public UserId expertFor(UserId named) {
    if (assignedExpertId.isPresent()) {
      if (!assignedExpertId.get().equals(named)) {
        throw new BusinessRuleException(
            EXPERT_CHANGED, "This case already has an expert. Please choose a time with them.");
      }
      return named;
    }
    if (eligibleExperts.stream().noneMatch(expert -> expert.id().equals(named))) {
      throw new BusinessRuleException(
          EXPERT_NOT_ELIGIBLE,
          "This expert is no longer available for this offering. Please choose another.");
    }
    return named;
  }

  /**
   * A slot has no free-cancellation time when the case's deadline is longer than the minimum
   * notice: that time has passed already (D-11).
   */
  public List<BookableSlot> bookableSlots(SlotFinder slotFinder, Instant now) {
    return slotFinder.startTimes(purchaseTerms.sessionDuration(), now).stream()
        .map(start -> new BookableSlot(start, endOf(start), freeCancellationUntil(start, now)))
        .toList();
  }

  private Optional<Instant> freeCancellationUntil(Instant start, Instant now) {
    return Optional.of(purchaseTerms.cancellationDeadline().freeUntil(start))
        .filter(until -> until.isAfter(now));
  }

  /** Checks every rule in this order: active case, open credit, expert, slot. */
  public Booking book(UserId expertId, Instant start, SlotFinder slotFinder, Instant now) {
    requireActive();
    requireOpenCredit();
    UserId expert = expertFor(expertId);
    if (!slotFinder.isBookable(start, purchaseTerms.sessionDuration(), now)) {
      throw new BusinessRuleException(SLOT_NOT_AVAILABLE, "This time is no longer available.");
    }
    Appointment appointment =
        new Appointment(new AppointmentId(UUID.randomUUID()), id, start, endOf(start));
    return new Booking(appointment, expert, assignedExpertId.isEmpty());
  }

  private Instant endOf(Instant start) {
    return start.plus(Duration.ofMinutes(purchaseTerms.sessionDuration().minutes()));
  }
}
