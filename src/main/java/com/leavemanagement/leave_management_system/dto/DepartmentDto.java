package com.leavemanagement.leave_management_system.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    private UUID id;
    private String name;
    private String description;
    private UUID headId;
    private String headName;
    private int numberOfEmployees;
}