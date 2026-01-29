package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.DepartmentUpsertRequest;
import com.universityofengineers.sms.dto.response.ApiMessageResponse;
import com.universityofengineers.sms.dto.response.DepartmentResponse;
import com.universityofengineers.sms.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentResponse> list() {
        return departmentService.list();
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Long id) {
        return departmentService.get(id);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public DepartmentResponse create(@Valid @RequestBody DepartmentUpsertRequest req) {
        return departmentService.create(req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentUpsertRequest req) {
        return departmentService.update(id, req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ApiMessageResponse delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiMessageResponse.builder()
                .timestamp(Instant.now())
                .message("Department deleted.")
                .build();
    }
}
