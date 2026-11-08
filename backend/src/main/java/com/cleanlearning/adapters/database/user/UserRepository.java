package com.cleanlearning.adapters.database.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.cleanlearning.adapters.database.user.entity.UserEntity;
import com.cleanlearning.adapters.database.user.row.UserRoleRow;

interface UserRepository extends Repository<UserEntity, UUID> {

    /**
     * Every user with their roles in one query, sorted by name, so the rows of one user are next to each other.
     */
    @Query("""
            select new com.cleanlearning.adapters.database.user.row.UserRoleRow(u.id, u.name, u.email, r.role)
            from UserEntity u
            left join UserRoleEntity r on r.userId = u.id
            order by u.name, u.id
            """)
    List<UserRoleRow> findAllWithRoles();

    /**
     * The user with their roles in one query: a row per role.
     */
    @Query("""
            select new com.cleanlearning.adapters.database.user.row.UserRoleRow(u.id, u.name, u.email, r.role)
            from UserEntity u
            left join UserRoleEntity r on r.userId = u.id
            where u.id = :id
            """)
    List<UserRoleRow> findWithRoles(UUID id);
}
