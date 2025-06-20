package com.vuviet.userservice.repository;

import com.vuviet.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByRefreshToken(String RefreshToken);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRoleName(String roleName);

    List<User> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE User u SET u.refreshToken=null, u.refreshTokenExpiry=null WHERE u.id=:userId")
    void clearRefreshToken(@Param("userId") long userId);
}
