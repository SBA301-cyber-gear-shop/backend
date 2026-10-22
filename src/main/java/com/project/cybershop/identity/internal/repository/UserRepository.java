package com.project.cybershop.identity.internal.repository;

import com.project.cybershop.identity.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query(value = """
            SELECT id FROM users
            WHERE id = :userId AND is_active = true
            FOR UPDATE
            """, nativeQuery = true)
    Optional<Integer> findActiveIdForUpdate(@Param("userId") Integer userId);

    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identity) OR u.phone = :identity")
    Optional<User> findByIdentity(@Param("identity") String identity);
}
