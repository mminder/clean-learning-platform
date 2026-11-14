package com.cleanlearning.adapters.database.user;

import com.cleanlearning.adapters.database.user.entity.UserEntity;
import com.cleanlearning.adapters.database.user.row.ExpertRow;
import com.cleanlearning.adapters.database.user.row.UserRoleRow;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface UserRepository extends Repository<UserEntity, UUID> {

  /** Sorted by name, so the rows of one user are next to each other. */
  @Query(
      """
      select new com.cleanlearning.adapters.database.user.row.UserRoleRow(u.id, u.name, u.email, r.role)
      from UserEntity u
      left join UserRoleEntity r on r.userId = u.id
      order by u.name, u.id
      """)
  List<UserRoleRow> findAllWithRoles();

  @Query(
      """
      select new com.cleanlearning.adapters.database.user.row.UserRoleRow(u.id, u.name, u.email, r.role)
      from UserEntity u
      left join UserRoleEntity r on r.userId = u.id
      where u.id = :id
      """)
  List<UserRoleRow> findWithRoles(UUID id);

  @Query(
      """
      select new com.cleanlearning.adapters.database.user.row.ExpertRow(p.userId, p.displayName)
      from ExpertProfileEntity p
      join UserRoleEntity r on r.userId = p.userId
      where r.role = com.cleanlearning.domain.user.value.Role.EXPERT
      order by p.displayName, p.userId
      """)
  List<ExpertRow> findExperts();
}
