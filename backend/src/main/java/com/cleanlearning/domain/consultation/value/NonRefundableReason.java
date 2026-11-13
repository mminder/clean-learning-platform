package com.cleanlearning.domain.consultation.value;

/**
 * Why cancelling a case doesn't refund its purchase price (UC-08), in the order the dialog gives
 * them: the first that applies is the reason.
 */
public enum NonRefundableReason {
  SESSION_TOOK_PLACE,
  CREDIT_FORFEITED,
  DEADLINE_PASSED
}
