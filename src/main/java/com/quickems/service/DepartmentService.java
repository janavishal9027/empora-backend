package com.quickems.service;

import com.quickems.dto.DepartmentDto;
import com.quickems.dto.DepartmentRequest;
import com.quickems.entity.Department;
import com.quickems.exception.DuplicateResourceException;
import com.quickems.exception.ResourceNotFoundException;
import com.quickems.repository.DepartmentRepository;
import com.quickems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public DepartmentDto getDepartmentById(Long id) {
        return toDto(findById(id));
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department with name '" + request.getName() + "' already exists");
        }
        Department dept = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toDto(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentRequest request) {
        Department dept = findById(id);
        if (!dept.getName().equals(request.getName()) && departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department with name '" + request.getName() + "' already exists");
        }
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        return toDto(departmentRepository.save(dept));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = findById(id);
        long empCount = employeeRepository.countByDepartmentId(id);
        if (empCount > 0) {
            throw new IllegalStateException("Cannot delete department with " + empCount + " employee(s). Reassign employees first.");
        }
        departmentRepository.delete(dept);
    }

    private Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private DepartmentDto toDto(Department dept) {
        return DepartmentDto.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .employeeCount(employeeRepository.countByDepartmentId(dept.getId()))
                .build();
    }
}
