package com.universityofengineers.sms.dto.request;

import com.universityofengineers.sms.entity.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentStatusUpdateRequest {
    @NotNull
    private StudentStatus status;
}
