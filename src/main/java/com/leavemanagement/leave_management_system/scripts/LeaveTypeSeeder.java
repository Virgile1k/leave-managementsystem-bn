package com.leavemanagement.leave_management_system.scripts;

import com.leavemanagement.leave_management_system.model.LeaveType;
import com.leavemanagement.leave_management_system.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Seeder class for populating the leave_types table with default leave types
 * according to Rwandan labor laws (Law N° 66/2018 of 30/08/2018).
 */
@Component
public class LeaveTypeSeeder implements CommandLineRunner {

    private final LeaveTypeRepository leaveTypeRepository;

    @Autowired
    public LeaveTypeSeeder(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    public void run(String... args) {
        // Only seed if the table is empty
        if (leaveTypeRepository.count() == 0) {
            seedLeaveTypes();
        }
    }

    private void seedLeaveTypes() {
        List<LeaveType> leaveTypes = Arrays.asList(
                // Annual Leave (18 working days per year according to Rwandan labor law)
                LeaveType.builder()
                        .name("Annual Leave")
                        .description("Regular paid time off for rest and recreation as per Law N° 66/2018")
                        .accrualRate(new BigDecimal("1.5")) // 1.5 days per month
                        .requiresDoc(false)
                        .maxDays(18)
                        .isActive(true)
                        .build(),

                // Maternity Leave (12 weeks according to Rwandan labor law)
                LeaveType.builder()
                        .name("Maternity Leave")
                        .description("Leave granted to female employees for childbirth and care of newborns")
                        .accrualRate(new BigDecimal("0"))  // Not accrued, granted as a block
                        .requiresDoc(true)
                        .maxDays(84)  // 12 weeks = 84 days
                        .isActive(true)
                        .build(),

                // Paternity Leave (4 consecutive days according to Rwandan labor law)
                LeaveType.builder()
                        .name("Paternity Leave")
                        .description("Leave granted to male employees upon birth of their child")
                        .accrualRate(new BigDecimal("0"))  // Not accrued, granted as a block
                        .requiresDoc(true)
                        .maxDays(4)
                        .isActive(true)
                        .build(),

                // Sick Leave
                LeaveType.builder()
                        .name("Sick Leave")
                        .description("Leave granted for medical reasons with a medical certificate")
                        .accrualRate(new BigDecimal("0"))  // Not accrued
                        .requiresDoc(true)
                        .maxDays(30)  // Common practice, may vary
                        .isActive(true)
                        .build(),

                // Compassionate Leave
                LeaveType.builder()
                        .name("Compassionate Leave")
                        .description("Leave granted for death or serious illness of close family members")
                        .accrualRate(new BigDecimal("0"))  // Not accrued
                        .requiresDoc(true)
                        .maxDays(5)  // Common practice, may vary
                        .isActive(true)
                        .build(),

                // Study Leave
                LeaveType.builder()
                        .name("Study Leave")
                        .description("Leave granted for educational or professional development purposes")
                        .accrualRate(new BigDecimal("0"))  // Not accrued
                        .requiresDoc(true)
                        .maxDays(14)  // Common practice, may vary
                        .isActive(true)
                        .build(),

                // Marriage Leave
                LeaveType.builder()
                        .name("Marriage Leave")
                        .description("Leave granted to employees on the occasion of their marriage")
                        .accrualRate(new BigDecimal("0"))  // Not accrued
                        .requiresDoc(true)
                        .maxDays(3)  // Common practice, may vary
                        .isActive(true)
                        .build(),

                // Unpaid Leave
                LeaveType.builder()
                        .name("Unpaid Leave")
                        .description("Leave without pay for personal reasons")
                        .accrualRate(new BigDecimal("0"))  // Not accrued
                        .requiresDoc(false)
                        .maxDays(90)  // Maximum duration may vary
                        .isActive(true)
                        .build()
        );

        leaveTypeRepository.saveAll(leaveTypes);
    }
}