package com.appname.authservice.repository;

import com.appname.authservice.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {

    Optional<UserCredentials> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByUserId(Long userId);

    Optional<UserCredentials> findByUserId(Long userId);

    // Native SQL — updated_at set explicitly to bypass JPA Auditing gap with native queries
    @Modifying
    @Query(value = "UPDATE user_credentials SET active = :active, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
            nativeQuery = true)
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
