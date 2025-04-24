package com.leavemanagement.leave_management_system.scripts;

import com.leavemanagement.leave_management_system.enums.UserRole;
import com.leavemanagement.leave_management_system.model.Department;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.DepartmentRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
public class UserSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            // Only delete users if we're doing a complete refresh
            // userRepository.deleteAll();
            // System.out.println("All existing users deleted for fresh reload");

            // Create and save departments first
            Department adminDept = createOrGetDepartment("Administration", "System Administration Department");
            Department hrDept = createOrGetDepartment("Human Resources", "Human Resources Department");
            Department techDept = createOrGetDepartment("Technology", "Technology Department");
            Department financeDept = createOrGetDepartment("Finance", "Finance Department");
            Department marketingDept = createOrGetDepartment("Marketing", "Marketing Department");

            System.out.println("Departments created or retrieved from database");

            // Create admin users in Administration department
            User admin = createOrUpdateUser(
                    "ndayambaje.virgile@techsroutine.com",
                    "System Administrator",
                    "admin123",
                    UserRole.ADMIN,
                    null,
                    adminDept,
                    "https://randomuser.me/api/portraits/men/1.jpg"
            );

            User virgileAdmin = createOrUpdateUser(
                    "ndayambajevg16bussiness@gmail.com",
                    "Virgile Ndayambaje",
                    "password123",
                    UserRole.ADMIN,
                    null,
                    adminDept,
                    "https://randomuser.me/api/portraits/men/2.jpg"
            );

            System.out.println("Admin users created or updated");

            // Create managers first
            User hrManager = createOrUpdateUser(
                    "hr.manager@company.com",
                    "HR Manager",
                    "password123",
                    UserRole.MANAGER,
                    null,
                    hrDept,
                    "https://randomuser.me/api/portraits/lego/1.jpg"
            );

            User techManager = createOrUpdateUser(
                    "tech.manager@company.com",
                    "Tech Manager",
                    "password123",
                    UserRole.MANAGER,
                    null,
                    techDept,
                    "https://randomuser.me/api/portraits/lego/1.jpg"
            );

            User financeManager = createOrUpdateUser(
                    "finance.manager@company.com",
                    "Finance Manager",
                    "password123",
                    UserRole.MANAGER,
                    null,
                    financeDept,
                    "https://randomuser.me/api/portraits/lego/1.jpg"
            );

            User marketingManager = createOrUpdateUser(
                    "marketing.manager@company.com",
                    "Marketing Manager",
                    "password123",
                    UserRole.MANAGER,
                    null,
                    marketingDept,
                    "https://randomuser.me/api/portraits/lego/1.jpg"
            );

            System.out.println("Managers created or updated");

            // Create or update employees for each department
            createOrUpdateUser("hr.employee1@company.com", "HR Employee 1", "password123", UserRole.STAFF, hrManager, hrDept, "https://randomuser.me/api/portraits/lego/1.jpg");
            createOrUpdateUser("hr.employee2@company.com", "HR Employee 2", "password123", UserRole.STAFF, hrManager, hrDept, "https://randomuser.me/api/portraits/lego/1.jpg");

            createOrUpdateUser("developer1@company.com", "Developer 1", "password123", UserRole.STAFF, techManager, techDept, "https://randomuser.me/api/portraits/lego/1.jpg");
            createOrUpdateUser("developer2@company.com", "Developer 2", "password123", UserRole.STAFF, techManager, techDept, "https://randomuser.me/api/portraits/lego/1.jpg");
            createOrUpdateUser("qa.engineer@company.com", "QA Engineer", "password123", UserRole.STAFF, techManager, techDept, "https://randomuser.me/api/portraits/lego/1.jpg");
            createOrUpdateUser("devops@company.com", "DevOps Engineer", "password123", UserRole.STAFF, techManager, techDept, "https://randomuser.me/api/portraits/lego/1.jpg");

            createOrUpdateUser("accountant1@company.com", "Accountant 1", "password123", UserRole.STAFF, financeManager, financeDept, "https://randomuser.me/api/portraits/lego/1.jpg");
            createOrUpdateUser("accountant2@company.com", "Accountant 2", "password123", UserRole.STAFF, financeManager, financeDept, "https://randomuser.me/api/portraits/lego/1.jpg");

            createOrUpdateUser("marketing.specialist1@company.com", "Marketing Specialist 1", "password123", UserRole.STAFF, marketingManager, marketingDept, "https://randomuser.me/api/portraits/lego/1.jpg");
            createOrUpdateUser("marketing.specialist2@company.com", "Marketing Specialist 2", "password123", UserRole.STAFF, marketingManager, marketingDept, "https://randomuser.me/api/portraits/lego/1.jpg");

            System.out.println("User seeding completed successfully");
        };
    }

    private Department createOrGetDepartment(String name, String description) {
        // Try to find department by name first
        Optional<Department> existingDept = departmentRepository.findByName(name);

        if (existingDept.isPresent()) {
            return existingDept.get();
        }

        // If not found, create new department
        Department department = Department.builder()
                .name(name)
                .description(description)
                .build();

        // Save the department to the database and return the saved entity
        return departmentRepository.save(department);
    }

    private User createOrUpdateUser(String email, String fullName, String rawPassword, UserRole role, User manager, Department department, String profilePicUrl) {
        // Check if user with this email already exists
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Update existing user
            User user = existingUser.get();
            user.setFullName(fullName);
            // Only update password if it's different (you might want to add more sophisticated password checking)
            if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
            user.setRole(role);
            user.setManager(manager);
            user.setDepartment(department);
            user.setProfilePicUrl(profilePicUrl);
            return userRepository.save(user);
        }

        // Create new user if not exists
        User newUser = User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .manager(manager)
                .department(department)
                .profilePicUrl(profilePicUrl)
                .build();

        return userRepository.save(newUser);
    }
}