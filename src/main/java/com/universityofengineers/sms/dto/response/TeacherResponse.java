package com.universityofengineers.sms.dto.response;

import com.universityofengineers.sms.entity.TeacherTitle;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherResponse {
    private Long id;
    private String employeeNo;
    private String fullName;
    private String email;
    private TeacherTitle title;
    private DepartmentResponse department;
}
