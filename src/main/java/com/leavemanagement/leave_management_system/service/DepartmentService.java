package com.leavemanagement.leave_management_system.service;

import com.leavemanagement.leave_management_system.dto.CreateDepartmentRequest;
import com.leavemanagement.leave_management_system.dto.DepartmentDto;
import com.leavemanagement.leave_management_system.dto.UpdateDepartmentRequest;
import com.leavemanagement.leave_management_system.exceptions.ResourceNotFoundException; // Updated import
import com.leavemanagement.leave_management_system.model.Department;
import com.leavemanagement.leave_management_system.model.User;
import com.leavemanagement.leave_management_system.repository.DepartmentRepository;
import com.leavemanagement.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDepartmentDto)
                .collect(Collectors.toList());
    }

    public DepartmentDto getDepartmentById(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return mapToDepartmentDto(department);
    }

    @Transactional
    public DepartmentDto createDepartment(CreateDepartmentRequest request) {
        // Check if department with same name already exists
        if (departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department with name " + request.getName() + " already exists");
        }

        User head = null;
        if (request.getHeadId() != null) {
            head = userRepository.findById(request.getHeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getHeadId()));
        }

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .head(head)
                .build();

        Department savedDepartment = departmentRepository.save(department);
        return mapToDepartmentDto(savedDepartment);
    }
    @Transactional
    public DepartmentDto assignDepartmentHead(UUID departmentId, UUID headId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        User head = userRepository.findById(headId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + headId));

        department.setHead(head);
        Department updatedDepartment = departmentRepository.save(department);
        return mapToDepartmentDto(updatedDepartment);
    }

    @Transactional
    public DepartmentDto removeDepartmentHead(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        department.setHead(null);
        Department updatedDepartment = departmentRepository.save(department);
        return mapToDepartmentDto(updatedDepartment);
    }

    @Transactional
    public DepartmentDto updateDepartment(UUID id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        // Check if updating to a name that already exists for another department
        if (request.getName() != null && !request.getName().equals(department.getName()) &&
                departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department with name " + request.getName() + " already exists");
        }

        if (request.getName() != null) {
            department.setName(request.getName());
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        if (request.getHeadId() != null) {
            User head = userRepository.findById(request.getHeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getHeadId()));
            department.setHead(head);
        }

        Department updatedDepartment = departmentRepository.save(department);
        return mapToDepartmentDto(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }

    public List<DepartmentDto> getDepartmentsByHead(UUID headId) {
        if (!userRepository.existsById(headId)) {
            throw new ResourceNotFoundException("User not found with id: " + headId);
        }

        return departmentRepository.findByHeadId(headId).stream()
                .map(this::mapToDepartmentDto)
                .collect(Collectors.toList());
    }

    private DepartmentDto mapToDepartmentDto(Department department) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());

        if (department.getHead() != null) {
            dto.setHeadId(department.getHead().getId());
            dto.setHeadName(department.getHead().getFullName());
        }

        if (department.getUsers() != null) {
            dto.setNumberOfEmployees(department.getUsers().size());
        } else {
            dto.setNumberOfEmployees(0);
        }

        return dto;
    }
}