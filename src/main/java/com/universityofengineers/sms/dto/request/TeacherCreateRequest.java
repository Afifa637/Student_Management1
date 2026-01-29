package com.universityofengineers.sms.dto.request;

import com.universityofengineers.sms.entity.TeacherTitle;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherCreateRequest {

    @NotBlank
    @Size(max = 150)
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotNull
    private Long departmentId;

    @NotNull
    private TeacherTitle title;

    private LocalDate hireDate;
}
