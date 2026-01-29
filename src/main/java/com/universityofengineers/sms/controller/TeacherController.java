package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.TeacherCreateRequest;
import com.universityofengineers.sms.dto.request.TeacherUpdateRequest;
import com.universityofengineers.sms.dto.response.ApiMessageResponse;
import com.universityofengineers.sms.dto.response.TeacherResponse;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    public List<TeacherResponse> list() {
        return teacherService.list();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}")
    public TeacherResponse get(@PathVariable Long id) {
        return teacherService.get(id);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/me")
    public TeacherResponse me() {
        return teacherService.me();
    }

    /**
     * Teachers are created by an existing teacher.
     * (No public "signup as teacher" endpoint exists.)
     */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public TeacherResponse create(@Valid @RequestBody TeacherCreateRequest req) {
        return teacherService.create(req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateRequest req) {
        return teacherService.update(id, req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ApiMessageResponse disable(@PathVariable Long id) {
        // Practical: prevent a teacher from disabling themselves and locking the system
        Long myId = teacherService.getCurrentTeacherEntity().getId();
        if (myId.equals(id)) {
            throw new BadRequestException("You cannot disable your own teacher account.");
        }
        teacherService.disableTeacher(id);
        return ApiMessageResponse.builder()
                .timestamp(Instant.now())
                .message("Teacher account disabled.")
                .build();
    }
}
