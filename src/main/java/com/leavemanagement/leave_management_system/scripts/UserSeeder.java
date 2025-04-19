package com.leavemanagement.leave_management_system.scripts;

import com.leavemanagement.leave_management_system.enums.UserRole;
import com.leavemanagement.leave_management_system.model.Department;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class UserSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            if (userRepository.count() == 0) {
                // Create departments (without repository)
                Department hrDept = createDummyDepartment("Human Resources");
                Department techDept = createDummyDepartment("Technology");
                Department financeDept = createDummyDepartment("Finance");
                Department marketingDept = createDummyDepartment("Marketing");

                // Create admin user
                User admin = User.builder()
                        .email("admin@company.com")
                        .fullName("System Administrator")
                        .password(passwordEncoder.encode("admin123"))
                        .role(UserRole.ADMIN)
                        .profilePicUrl("https://randomuser.me/api/portraits/men/1.jpg")
                        .build();

                userRepository.save(admin);
                System.out.println("Admin user created");

                // Create managers first
                User hrManager = createUser("hr.manager@company.com", "HR Manager", "password123", UserRole.MANAGER, null, hrDept);
                User techManager = createUser("tech.manager@company.com", "Tech Manager", "password123", UserRole.MANAGER, null, techDept);
                User financeManager = createUser("finance.manager@company.com", "Finance Manager", "password123", UserRole.MANAGER, null, financeDept);
                User marketingManager = createUser("marketing.manager@company.com", "Marketing Manager", "password123", UserRole.MANAGER, null, marketingDept);

                // Save managers
                List<User> managers = new ArrayList<>();
                managers.add(hrManager);
                managers.add(techManager);
                managers.add(financeManager);
                managers.add(marketingManager);
                List<User> savedManagers = userRepository.saveAll(managers);

                // Get the saved managers with their IDs
                User savedHrManager = savedManagers.get(0);
                User savedTechManager = savedManagers.get(1);
                User savedFinanceManager = savedManagers.get(2);
                User savedMarketingManager = savedManagers.get(3);

                // Create employees for each department
                List<User> employees = new ArrayList<>();

                // HR department employees
                employees.add(createUser("hr.employee1@company.com", "HR Employee 1", "password123", UserRole.STAFF, savedHrManager, hrDept));
                employees.add(createUser("hr.employee2@company.com", "HR Employee 2", "password123", UserRole.STAFF, savedHrManager, hrDept));

                // Tech department employees
                employees.add(createUser("developer1@company.com", "Developer 1", "password123", UserRole.STAFF, savedTechManager, techDept));
                employees.add(createUser("developer2@company.com", "Developer 2", "password123", UserRole.STAFF, savedTechManager, techDept));
                employees.add(createUser("qa.engineer@company.com", "QA Engineer", "password123", UserRole.STAFF, savedTechManager, techDept));
                employees.add(createUser("devops@company.com", "DevOps Engineer", "password123", UserRole.STAFF, savedTechManager, techDept));

                // Finance department employees
                employees.add(createUser("accountant1@company.com", "Accountant 1", "password123", UserRole.STAFF, savedFinanceManager, financeDept));
                employees.add(createUser("accountant2@company.com", "Accountant 2", "password123", UserRole.STAFF, savedFinanceManager, financeDept));

                // Marketing department employees
                employees.add(createUser("marketing.specialist1@company.com", "Marketing Specialist 1", "password123", UserRole.STAFF, savedMarketingManager, marketingDept));
                employees.add(createUser("marketing.specialist2@company.com", "Marketing Specialist 2", "password123", UserRole.STAFF, savedMarketingManager, marketingDept));

                userRepository.saveAll(employees);

                System.out.println("User seeding completed successfully");
            } else {
                System.out.println("Users already exist, skipping seeder");
            }
        };
    }

    private Department createDummyDepartment(String name) {
        return Department.builder()
                .id(UUID.randomUUID())  // Generate random UUID for department
                .name(name)
                .description(name + " Department")
                .build();
    }

    private User createUser(String email, String fullName, String rawPassword, UserRole role, User manager, Department department) {
        return User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .manager(manager)
                .department(department)
                .profilePicUrl("https://randomuser.me/api/portraits/lego/1.jpg")
                .build();
    }
}