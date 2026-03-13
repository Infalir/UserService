package com.appname.userservice.repository;

import com.appname.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
  Page<User> findByActive(Boolean active, Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.active = true AND (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))")
  Page<User> findActiveByNameContaining(@Param("name") String name, Pageable pageable);

  @Modifying
  @Query(value = "UPDATE users SET active = :active WHERE id = :id", nativeQuery = true)
  int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);

}
