package com.universityofengineers.sms.dto.request;

import com.universityofengineers.sms.entity.TeacherTitle;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherUpdateMeRequest {

    private TeacherTitle title;
    private Long departmentId;
    private LocalDate hireDate;
}
