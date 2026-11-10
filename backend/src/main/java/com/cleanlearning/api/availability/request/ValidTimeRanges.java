package com.cleanlearning.api.availability.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * UC-13's rules for the time ranges of an {@link AvailabilityRequest}, with a violation per invalid range, so the page
 * shows each message next to its field: at {@code timeRanges[i].end} for a range whose end isn't after its start, and
 * at {@code timeRanges[i].start} for the later-starting of two ranges that overlap or touch (spec 04, decision 9). The
 * domain's {@code TimeRange} and {@code Availability} hold the same rules.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidTimeRangesValidator.class)
public @interface ValidTimeRanges {

    String message() default "must have valid time ranges";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
