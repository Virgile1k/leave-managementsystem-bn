package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email address
     * @param email The email address to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the specified email exists
     * @param email The email to check
     * @return true if a user exists with this email, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all users that have a specific manager
     * @param managerId The ID of the manager
     * @return List of users managed by the specified manager
     */
    List<User> findByManagerId(UUID managerId);

    /**
     * Find all users belonging to a specific department
     * @param departmentId The ID of the department
     * @return List of users in the specified department
     */
    List<User> findByDepartmentId(UUID departmentId);

    /**
     * Find users by role
     * @param role The role to search for
     * @return List of users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") String role);

    /**
     * Search for users by name or email
     * @param searchTerm The search term to match against name or email
     * @return List of users matching the search criteria
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Find all managers (users who manage others)
     * @return List of users who are managers
     */
    @Query("SELECT DISTINCT u.manager FROM User u WHERE u.manager IS NOT NULL")
    List<User> findAllManagers();
}