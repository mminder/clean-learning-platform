package com.cleanlearning.domain.consultation.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class BookableSlotTest {

    private static final Instant TEN = Instant.parse("2026-11-17T09:00:00Z");
    private static final Instant ELEVEN = Instant.parse("2026-11-17T10:00:00Z");
    private static final Optional<Instant> DAY_BEFORE = Optional.of(Instant.parse("2026-11-16T09:00:00Z"));

    @Test
    void holdsItsStartEndAndFreeCancellationTime() {
        BookableSlot slot = new BookableSlot(TEN, ELEVEN, DAY_BEFORE);

        assertThat(slot.start()).isEqualTo(TEN);
        assertThat(slot.end()).isEqualTo(ELEVEN);
        assertThat(slot.freeCancellationUntil()).isEqualTo(DAY_BEFORE);
    }

    @Test
    void mayHaveNoFreeCancellationTime() {
        assertThat(new BookableSlot(TEN, ELEVEN, Optional.empty()).freeCancellationUntil()).isEmpty();
    }

    @Test
    void endsAfterItStarts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BookableSlot(ELEVEN, TEN, DAY_BEFORE))
                .withMessage("A bookable slot ends after it starts: " + ELEVEN + " to " + TEN + ".");
        assertThatIllegalArgumentException().isThrownBy(() -> new BookableSlot(TEN, TEN, DAY_BEFORE));
        assertThatIllegalArgumentException().isThrownBy(() -> new BookableSlot(TEN, TEN, Optional.empty()));
    }

    @Test
    void isFreeToCancelUntilItsStartAtTheLatest() {
        assertThat(new BookableSlot(TEN, ELEVEN, Optional.of(TEN)).freeCancellationUntil()).contains(TEN);
        assertThatIllegalArgumentException().isThrownBy(() -> new BookableSlot(TEN, ELEVEN, Optional.of(ELEVEN)))
                .withMessage("Free cancellation ends by the start: " + ELEVEN + " after " + TEN + ".");
    }

    @Test
    void needsAllThree() {
        assertThatNullPointerException().isThrownBy(() -> new BookableSlot(null, ELEVEN, DAY_BEFORE))
                .withMessage("start");
        assertThatNullPointerException().isThrownBy(() -> new BookableSlot(TEN, null, DAY_BEFORE))
                .withMessage("end");
        assertThatNullPointerException().isThrownBy(() -> new BookableSlot(TEN, ELEVEN, null))
                .withMessage("freeCancellationUntil");
    }
}
