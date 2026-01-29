package com.universityofengineers.sms.dto.response;

import com.universityofengineers.sms.entity.StudentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private String studentNo;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private StudentStatus status;
    private DepartmentResponse department;
}
