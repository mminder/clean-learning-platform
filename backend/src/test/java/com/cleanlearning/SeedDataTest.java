package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The {@code poc} seed holds the six users of UC-01, under the ids in {@link SeedData}. Their roles are checked through
 * {@code GET /api/poc/users} ({@code PocLoginControllerTest}); the API shows every user's email only to the user, so
 * this test checks the emails in the rows.
 */
@IntegrationTest
class SeedDataTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    void namesTheUsersAsUseCaseOneDoes() {
        Map<UUID, String> userById = jdbc.sql("select id, name, email from app_user")
                .query((row, rowNumber) -> Map.entry(row.getObject("id", UUID.class),
                        row.getString("name") + " <" + row.getString("email") + ">"))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(userById).containsOnly(
                entry(SeedData.CUSTOMER_1, "Customer 1 <customer1@example.com>"),
                entry(SeedData.CUSTOMER_2, "Customer 2 <customer2@example.com>"),
                entry(SeedData.EXPERT_1, "Expert 1 <expert1@example.com>"),
                entry(SeedData.EXPERT_2, "Expert 2 <expert2@example.com>"),
                entry(SeedData.ADMIN_1, "Admin 1 <admin1@example.com>"),
                entry(SeedData.ADMIN_2, "Admin 2 <admin2@example.com>"));
    }
}
