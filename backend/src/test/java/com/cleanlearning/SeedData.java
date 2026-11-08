package com.cleanlearning;

import java.util.UUID;

/**
 * The fixed ids of the {@code poc} seed in {@code db/seed}, see the seed ids in
 * _docs/tasks/01-shop-tracer-bullet/README.md.
 */
public final class SeedData {

    public static final UUID CUSTOMER_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID CUSTOMER_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    public static final UUID EXPERT_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    public static final UUID EXPERT_2 = UUID.fromString("00000000-0000-0000-0000-000000000202");
    public static final UUID ADMIN_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    public static final UUID ADMIN_2 = UUID.fromString("00000000-0000-0000-0000-000000000302");

    public static final UUID CAREER_COACHING = UUID.fromString("00000000-0000-0000-0000-000000001001");
    public static final UUID CAREER_COACHING_3_SESSIONS = UUID.fromString("00000000-0000-0000-0000-000000001011");
    public static final UUID CAREER_COACHING_5_SESSIONS = UUID.fromString("00000000-0000-0000-0000-000000001012");

    /**
     * The script that deletes every offering that isn't in the seed, with its packages and eligible experts. A test
     * that writes offerings runs it afterwards: {@code @Sql(scripts = DELETE_TEST_OFFERINGS, executionPhase =
     * AFTER_TEST_METHOD)}.
     */
    public static final String DELETE_TEST_OFFERINGS = "/com/cleanlearning/delete-test-offerings.sql";

    private SeedData() {
    }
}
