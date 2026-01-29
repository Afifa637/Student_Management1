package com.universityofengineers.sms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentUpdateMeRequest {

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String address;
}
