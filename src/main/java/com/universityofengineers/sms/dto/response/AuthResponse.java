package com.universityofengineers.sms.dto.response;

import com.universityofengineers.sms.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInMillis;

    private Role role;
    private Long accountId;
    private String email;

    // convenience fields
    private Long studentId;
    private Long teacherId;
}
