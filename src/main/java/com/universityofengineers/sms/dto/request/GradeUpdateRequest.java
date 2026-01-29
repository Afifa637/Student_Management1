package com.universityofengineers.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GradeUpdateRequest {
    @NotBlank
    @Size(max = 5)
    private String grade; // e.g., A, A-, B+, etc.
}
