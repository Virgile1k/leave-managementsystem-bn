package com.leavemanagement.leave_management_system.controller;

import com.leavemanagement.leave_management_system.dto.CreateDepartmentRequest;
import com.leavemanagement.leave_management_system.dto.DepartmentDto;
import com.leavemanagement.leave_management_system.dto.UpdateDepartmentRequest;
import com.leavemanagement.leave_management_system.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentDto createdDepartment = departmentService.createDepartment(request);
        return new ResponseEntity<>(createdDepartment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDto> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-head/{headId}")
    public ResponseEntity<List<DepartmentDto>> getDepartmentsByHead(@PathVariable UUID headId) {
        List<DepartmentDto> departments = departmentService.getDepartmentsByHead(headId);
        return ResponseEntity.ok(departments);
    }

    // Department head management endpoints
    @PutMapping("/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDto> assignDepartmentHead(
            @PathVariable UUID id,
            @RequestParam UUID headId) {
        return ResponseEntity.ok(departmentService.assignDepartmentHead(id, headId));
    }

    @DeleteMapping("/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDto> removeDepartmentHead(
            @PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.removeDepartmentHead(id));
    }
}