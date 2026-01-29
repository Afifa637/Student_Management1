package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.PasswordResetRequest;
import com.universityofengineers.sms.dto.request.StudentStatusUpdateRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateMeRequest;
import com.universityofengineers.sms.dto.response.ApiMessageResponse;
import com.universityofengineers.sms.dto.response.StudentResponse;
import com.universityofengineers.sms.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // Teacher operations
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    public List<StudentResponse> list() {
        return studentService.list();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return studentService.get(id);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}/status")
    public StudentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StudentStatusUpdateRequest req) {
        return studentService.updateStatus(id, req);
    }

    /**
     * Rule: student cannot delete own account; only teacher can do it.
     * We provide this endpoint ONLY for TEACHER.
     */
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ApiMessageResponse disable(@PathVariable Long id) {
        studentService.disableStudent(id);
        return ApiMessageResponse.builder()
                .timestamp(Instant.now())
                .message("Student account disabled (soft delete).")
                .build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{id}/reset-password")
    public ApiMessageResponse resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest req) {
        studentService.resetStudentPassword(id, req);
        return ApiMessageResponse.builder()
                .timestamp(Instant.now())
                .message("Student password reset.")
                .build();
    }

    // Student self-service operations
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public StudentResponse me() {
        return studentService.me();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/me")
    public StudentResponse updateMe(@Valid @RequestBody StudentUpdateMeRequest req) {
        return studentService.updateMe(req);
    }
}
