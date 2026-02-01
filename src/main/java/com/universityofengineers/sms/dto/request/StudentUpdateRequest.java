package com.universityofengineers.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentUpdateRequest {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotNull
    private Long departmentId;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String address;

    private LocalDate dateOfBirth;
}
