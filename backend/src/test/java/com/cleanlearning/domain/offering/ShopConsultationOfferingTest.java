package com.cleanlearning.domain.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.shared.Money;
import com.cleanlearning.domain.user.UserId;

class ShopConsultationOfferingTest {

    private static final OfferingId ID = new OfferingId(UUID.fromString("00000000-0000-0000-0000-000000001001"));
    private static final SessionDuration SIXTY_MINUTES = new SessionDuration(60);
    private static final CancellationDeadline ONE_DAY = new CancellationDeadline(24);
    private static final ConsultationPackage THREE_SESSIONS = new ConsultationPackage(
            new ConsultationPackageId(UUID.fromString("00000000-0000-0000-0000-000000001011")), 3,
            Money.chf("450.00"));
    private static final EligibleExpert EXPERT_1 = new EligibleExpert(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000201")), "Expert 1", "Career coach.");

    @Test
    void holdsWhatTheDetailPageShows() {
        ShopConsultationOffering offering = new ShopConsultationOffering(ID, "Career Coaching", "Coaching.",
                SIXTY_MINUTES, ONE_DAY, List.of(THREE_SESSIONS), List.of(EXPERT_1));

        assertThat(offering.type()).isEqualTo(OfferingType.CONSULTATION);
        assertThat(offering.sessionDuration()).isEqualTo(SIXTY_MINUTES);
        assertThat(offering.cancellationDeadline()).isEqualTo(ONE_DAY);
        assertThat(offering.packages()).containsExactly(THREE_SESSIONS);
        assertThat(offering.eligibleExperts()).containsExactly(EXPERT_1);
    }

    @Test
    void cannotBeChangedThroughItsLists() {
        List<ConsultationPackage> packages = new ArrayList<>(List.of(THREE_SESSIONS));
        ShopConsultationOffering offering = new ShopConsultationOffering(ID, "Career Coaching", "Coaching.",
                SIXTY_MINUTES, ONE_DAY, packages, List.of(EXPERT_1));

        packages.clear();

        assertThat(offering.packages()).containsExactly(THREE_SESSIONS);
        assertThatThrownBy(() -> offering.eligibleExperts().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMissingValues() {
        List<ConsultationPackage> packages = List.of(THREE_SESSIONS);
        List<EligibleExpert> experts = List.of(EXPERT_1);
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(null, "Name", "Text",
                SIXTY_MINUTES, ONE_DAY, packages, experts));
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(ID, null, "Text",
                SIXTY_MINUTES, ONE_DAY, packages, experts));
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(ID, "Name", null,
                SIXTY_MINUTES, ONE_DAY, packages, experts));
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(ID, "Name", "Text",
                null, ONE_DAY, packages, experts));
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(ID, "Name", "Text",
                SIXTY_MINUTES, null, packages, experts));
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(ID, "Name", "Text",
                SIXTY_MINUTES, ONE_DAY, null, experts));
        assertThatNullPointerException().isThrownBy(() -> new ShopConsultationOffering(ID, "Name", "Text",
                SIXTY_MINUTES, ONE_DAY, packages, null));
    }

    @Test
    void eligibleExpertNeedsAllItsValues() {
        UserId id = EXPERT_1.id();
        assertThatNullPointerException().isThrownBy(() -> new EligibleExpert(null, "Expert 1", "Bio"));
        assertThatNullPointerException().isThrownBy(() -> new EligibleExpert(id, null, "Bio"));
        assertThatNullPointerException().isThrownBy(() -> new EligibleExpert(id, "Expert 1", null));
        assertThatNullPointerException().isThrownBy(() -> new UserId(null));
    }
}
