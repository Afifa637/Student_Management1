package com.universityofengineers.sms.dto.response;

import com.universityofengineers.sms.entity.EnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EnrollmentResponse {
    private Long id;

    private Long studentId;
    private String studentNo;

    private Long courseId;
    private String courseCode;
    private String courseTitle;

    private EnrollmentStatus status;
    private String grade;
    private Instant enrolledAt;
}
