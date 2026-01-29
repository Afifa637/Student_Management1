package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.EnrollmentCreateRequest;
import com.universityofengineers.sms.dto.request.GradeUpdateRequest;
import com.universityofengineers.sms.dto.response.ApiMessageResponse;
import com.universityofengineers.sms.dto.response.EnrollmentResponse;
import com.universityofengineers.sms.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // Student self-service enrollment endpoints
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/enrollments/me")
    public List<EnrollmentResponse> myEnrollments() {
        return enrollmentService.myEnrollments();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/enrollments/me")
    public EnrollmentResponse enrollMe(@Valid @RequestBody EnrollmentCreateRequest req) {
        return enrollmentService.enrollMe(req);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/enrollments/me/{enrollmentId}")
    public ApiMessageResponse dropMe(@PathVariable Long enrollmentId) {
        enrollmentService.dropMyEnrollment(enrollmentId);
        return ApiMessageResponse.builder()
                .timestamp(Instant.now())
                .message("Enrollment dropped.")
                .build();
    }

    // Teacher enrollment management endpoints
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/enrollments")
    public List<EnrollmentResponse> listAll() {
        return enrollmentService.listAll();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/students/{studentId}/enrollments")
    public EnrollmentResponse enrollStudent(@PathVariable Long studentId, @Valid @RequestBody EnrollmentCreateRequest req) {
        return enrollmentService.teacherEnrollStudent(studentId, req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/enrollments/{enrollmentId}/grade")
    public EnrollmentResponse setGrade(@PathVariable Long enrollmentId, @Valid @RequestBody GradeUpdateRequest req) {
        return enrollmentService.setGrade(enrollmentId, req);
    }
}
