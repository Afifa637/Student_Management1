package com.universityofengineers.sms.dto.request;

import com.universityofengineers.sms.entity.TeacherTitle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherUpdateRequest {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotNull
    private TeacherTitle title;

    @NotNull
    private Long departmentId;

    private LocalDate hireDate;
}
