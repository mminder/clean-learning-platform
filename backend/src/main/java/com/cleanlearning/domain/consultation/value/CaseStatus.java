package com.cleanlearning.domain.consultation.value;

/**
 * A case's stored status (glossary, D-12): {@code ACTIVE} from its purchase, {@code CANCELLED} once
 * its customer cancelled it (UC-08). {@code COMPLETED} is derived at a time, never stored (D-11):
 * an active case without open credits and without a booked appointment that hasn't ended.
 */
public enum CaseStatus {
  ACTIVE,
  CANCELLED
}
