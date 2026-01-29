package com.universityofengineers.sms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private String code;
    private String title;
    private double credit;
    private int capacity;
    private DepartmentResponse department;
    private TeacherResponse teacher;
    private long currentlyEnrolled;
}
