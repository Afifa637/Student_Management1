package com.universityofengineers.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ApiMessageResponse {
    private Instant timestamp;
    private String message;
}
