package com.cleanlearning.api.availability.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * Checks {@link ValidTimeRanges}. A range with a missing time is left to {@code @NotNull}, and a
 * range whose end isn't after its start takes part in no overlap.
 */
public class ValidTimeRangesValidator
    implements ConstraintValidator<ValidTimeRanges, AvailabilityRequest> {

  public static final String END_NOT_AFTER_START = "must be after the start";

  public static final String OVERLAPS_OR_TOUCHES = "must not overlap or touch another time range";

  @Override
  public boolean isValid(AvailabilityRequest request, ConstraintValidatorContext context) {
    List<TimeRangeRequest> ranges = request.timeRanges();
    if (ranges == null) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    boolean valid = true;
    for (int i = 0; i < ranges.size(); i++) {
      if (!isComplete(ranges.get(i))) {
        continue;
      }
      if (!isOrdered(ranges.get(i))) {
        addViolation(context, i, "end", END_NOT_AFTER_START);
        valid = false;
      } else if (overlapsOrTouchesAnEarlierOne(ranges, i)) {
        addViolation(context, i, "start", OVERLAPS_OR_TOUCHES);
        valid = false;
      }
    }
    return valid;
  }

  /**
   * Whether another range starts before range {@code i}, or at the same time and earlier in the
   * list, and overlaps or touches it: then range {@code i} is the later-starting one of the two.
   */
  private static boolean overlapsOrTouchesAnEarlierOne(List<TimeRangeRequest> ranges, int i) {
    TimeRangeRequest range = ranges.get(i);
    for (int j = 0; j < ranges.size(); j++) {
      TimeRangeRequest other = ranges.get(j);
      if (j == i || !isComplete(other) || !isOrdered(other)) {
        continue;
      }
      boolean startsEarlier =
          other.start().isBefore(range.start()) || other.start().equals(range.start()) && j < i;
      boolean overlapsOrTouches =
          !range.start().isAfter(other.end()) && !other.start().isAfter(range.end());
      if (startsEarlier && overlapsOrTouches) {
        return true;
      }
    }
    return false;
  }

  private static boolean isComplete(TimeRangeRequest range) {
    return range != null && range.start() != null && range.end() != null;
  }

  private static boolean isOrdered(TimeRangeRequest range) {
    return range.start().isBefore(range.end());
  }

  /** A violation at {@code timeRanges[index].property}, the path Spring reports as the field. */
  private static void addViolation(
      ConstraintValidatorContext context, int index, String property, String message) {
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode("timeRanges")
        .addPropertyNode(property)
        .inIterable()
        .atIndex(index)
        .addConstraintViolation();
  }
}
