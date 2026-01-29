package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.DepartmentUpsertRequest;
import com.universityofengineers.sms.dto.response.DepartmentResponse;
import com.universityofengineers.sms.entity.Department;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> list() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public DepartmentResponse get(Long id) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found."));
        return toResponse(d);
    }

    @Transactional
    public DepartmentResponse create(DepartmentUpsertRequest req) {
        String code = req.getCode().trim().toUpperCase();
        if (departmentRepository.existsByCode(code)) {
            throw new BadRequestException("Department code already exists.");
        }
        Department d = Department.builder()
                .code(code)
                .name(req.getName().trim())
                .build();
        return toResponse(departmentRepository.save(d));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpsertRequest req) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        String code = req.getCode().trim().toUpperCase();
        if (!d.getCode().equals(code) && departmentRepository.existsByCode(code)) {
            throw new BadRequestException("Department code already exists.");
        }

        d.setCode(code);
        d.setName(req.getName().trim());
        return toResponse(departmentRepository.save(d));
    }

    @Transactional
    public void delete(Long id) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department not found."));
        departmentRepository.delete(d);
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .build();
    }
}
