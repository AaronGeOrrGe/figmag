package com.figmine.backend.repository;

import com.figmine.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // Basic CRUD operations are provided by JpaRepository
    
    // Find by email (case-insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);
    
    // Check if email exists (case-insensitive)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);
    
    // Find users by name (case-insensitive, partial match)
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find users created after a specific date
    List<User> findByCreatedAtAfter(Instant date);
    
    // Find users with onboarding completed
    List<User> findByOnboardingCompleteTrue();
    
    // Update last login timestamp
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") Instant lastLogin);
    
    // Check if a user exists and is enabled
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :userId AND u.enabled = true")
    boolean existsByIdAndEnabledTrue(@Param("userId") Long userId);
    
    // Count active users (enabled and not locked)
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.accountNonLocked = true")
    long countActiveUsers();
    
    // Find users by creation date range
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
