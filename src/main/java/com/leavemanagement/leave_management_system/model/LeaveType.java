package com.leavemanagement.leave_management_system.model;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;



@Entity
@Table(name = "leave_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "accrual_rate", nullable = true, precision = 5, scale = 2)
    private BigDecimal accrualRate;

    @Column(name = "requires_doc", nullable = false)
    private Boolean requiresDoc;

    @Column(name = "max_days", nullable = false)
    private Integer maxDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "leaveType")
    private Set<LeaveRequest> leaveRequests;

    @OneToMany(mappedBy = "leaveType")
    private Set<LeaveBalance> leaveBalances;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}