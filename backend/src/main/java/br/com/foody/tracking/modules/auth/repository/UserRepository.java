package br.com.foody.tracking.modules.auth.repository;

import br.com.foody.tracking.modules.auth.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  /** One statement for any number of ids: the order history resolves its actor names here. */
  List<UserNameView> findByPublicIdIn(Collection<UUID> publicIds);

  // The ADMIN exclusion lives in the query (a literal, not a parameter), so no caller can widen
  // the demo reset to the admin account. One short write: the BCrypt hash is computed before it.
  @Modifying
  @Transactional
  @Query(
      "update User u set u.passwordHash = :hash"
          + " where u.email = :email"
          + " and u.role = br.com.foody.tracking.modules.auth.enums.Role.USER")
  int updatePasswordHashForUserRole(@Param("email") String email, @Param("hash") String hash);

  boolean existsByEmail(String email);

  Optional<User> findByPublicId(UUID publicId);
}
