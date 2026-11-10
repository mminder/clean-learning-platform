package com.cleanlearning.domain.consultation.value;

/**
 * The group of "My purchases" a case is in (UC-05, D-17), in the order the page shows them: {@code UPCOMING} with an
 * appointment that hasn't ended, {@code NEEDS_BOOKING} with an open credit and nothing upcoming, and {@code PAST}
 * otherwise, a completed case. Computed at a time, never stored (D-11). UC-08 adds the cancelled cases to
 * {@code PAST}.
 */
public enum PurchaseGroup {
    UPCOMING, NEEDS_BOOKING, PAST
}
