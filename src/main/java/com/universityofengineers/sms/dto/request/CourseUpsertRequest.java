package com.universityofengineers.sms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CourseUpsertRequest {

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 160)
    private String title;

    @Positive
    private double credit;

    @Min(1)
    private int capacity = 60;

    @NotNull
    private Long departmentId;

    /**
     * Optional: if not provided, teacherId will default to the currently authenticated teacher.
     */
    private Long teacherId;
}
