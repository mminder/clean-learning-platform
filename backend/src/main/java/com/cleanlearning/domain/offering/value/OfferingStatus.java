package com.cleanlearning.domain.offering.value;

/**
 * Where an offering is in its life (UC-16): a draft is being prepared, a published one is in the
 * shop, an archived one is closed for good.
 */
public enum OfferingStatus {
  DRAFT,
  PUBLISHED,
  ARCHIVED
}
