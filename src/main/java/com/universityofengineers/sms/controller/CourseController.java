package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.CourseUpsertRequest;
import com.universityofengineers.sms.dto.response.ApiMessageResponse;
import com.universityofengineers.sms.dto.response.CourseResponse;
import com.universityofengineers.sms.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public List<CourseResponse> list() {
        return courseService.list();
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return courseService.get(id);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public CourseResponse create(@Valid @RequestBody CourseUpsertRequest req) {
        return courseService.create(req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseUpsertRequest req) {
        return courseService.update(id, req);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ApiMessageResponse delete(@PathVariable Long id) {
        courseService.delete(id);
        return ApiMessageResponse.builder()
                .timestamp(Instant.now())
                .message("Course deleted.")
                .build();
    }
}
