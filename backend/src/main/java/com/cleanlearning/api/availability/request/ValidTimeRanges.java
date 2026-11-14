package com.cleanlearning.api.availability.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One violation per invalid range, so the page shows each message next to its field: at {@code
 * timeRanges[i].end} for a range that doesn't end after its start, at {@code timeRanges[i].start}
 * for the later-starting of two that overlap or touch.
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
