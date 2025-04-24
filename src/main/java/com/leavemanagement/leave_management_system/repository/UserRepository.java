package com.leavemanagement.leave_management_system.repository;

import com.leavemanagement.leave_management_system.enums.UserRole;
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
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByManagerId(UUID managerId);

    List<User> findByDepartmentId(UUID departmentId);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") UserRole role);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    @Query("SELECT DISTINCT u.manager FROM User u WHERE u.manager IS NOT NULL")
    List<User> findAllManagers();

    // Updated queries to use role field instead of roles relationship
    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_MANAGER' AND u.department.id = :departmentId")
    List<User> findManagersByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_ADMIN'")
    List<User> findAllAdmins();
}