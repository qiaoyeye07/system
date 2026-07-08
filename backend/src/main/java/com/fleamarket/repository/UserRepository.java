package com.fleamarket.repository;

import com.fleamarket.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(com.fleamarket.domain.enums.UserRole role);

    List<User> findByRoleAndEnabled(com.fleamarket.domain.enums.UserRole role, Boolean enabled);
}
