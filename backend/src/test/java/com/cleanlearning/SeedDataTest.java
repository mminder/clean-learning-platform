package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The {@code poc} seed holds the six users of UC-01 with their roles, under the ids in {@link SeedData}. Nothing reads
 * the users through the API yet, so this test checks the rows directly.
 */
@IntegrationTest
class SeedDataTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    void holdsTwoUsersPerRoleWithTheirUseCaseRoles() {
        Map<UUID, List<String>> rolesByUser = jdbc
                .sql("select user_id, role from user_role order by role")
                .query((row, rowNumber) -> Map.entry(row.getObject("user_id", UUID.class), row.getString("role")))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        assertThat(rolesByUser).containsOnly(
                entry(SeedData.CUSTOMER_1, List.of("CUSTOMER")),
                entry(SeedData.CUSTOMER_2, List.of("CUSTOMER")),
                entry(SeedData.EXPERT_1, List.of("CUSTOMER", "EXPERT")),
                entry(SeedData.EXPERT_2, List.of("ADMIN", "EXPERT")),
                entry(SeedData.ADMIN_1, List.of("ADMIN")),
                entry(SeedData.ADMIN_2, List.of("ADMIN")));
    }

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
