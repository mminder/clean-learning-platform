package com.cleanlearning.domain.consultation.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;

class PurchaseTermsTest {

    private static final Money PRICE = Money.chf("450");
    private static final SessionCount THREE = new SessionCount(3);
    private static final SessionDuration SIXTY_MINUTES = new SessionDuration(60);
    private static final CancellationDeadline ONE_DAY = new CancellationDeadline(24);

    @Test
    void holdsTheFourTerms() {
        PurchaseTerms terms = new PurchaseTerms(PRICE, THREE, SIXTY_MINUTES, ONE_DAY);

        assertThat(terms.pricePaid()).isEqualTo(Money.chf("450.00"));
        assertThat(terms.sessionCount()).isEqualTo(THREE);
        assertThat(terms.sessionDuration()).isEqualTo(SIXTY_MINUTES);
        assertThat(terms.cancellationDeadline()).isEqualTo(ONE_DAY);
    }

    @Test
    void needsEveryTerm() {
        assertThatNullPointerException().isThrownBy(() -> new PurchaseTerms(null, THREE, SIXTY_MINUTES, ONE_DAY))
                .withMessage("pricePaid");
        assertThatNullPointerException().isThrownBy(() -> new PurchaseTerms(PRICE, null, SIXTY_MINUTES, ONE_DAY))
                .withMessage("sessionCount");
        assertThatNullPointerException().isThrownBy(() -> new PurchaseTerms(PRICE, THREE, null, ONE_DAY))
                .withMessage("sessionDuration");
        assertThatNullPointerException().isThrownBy(() -> new PurchaseTerms(PRICE, THREE, SIXTY_MINUTES, null))
                .withMessage("cancellationDeadline");
    }
}
