package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.StudentRegistrationRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateMeRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateRequest;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.support.IntegrationTestBase;
import com.universityofengineers.sms.support.SmsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SmsIntegrationTest
class StudentControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void list_shouldReturn401_whenNoJwtProvided() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void list_shouldReturn403_whenStudentRoleTriesTeacherEndpoint() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenStudent("student@ue.edu", "Secret123!", dept, studentNo(1), StudentStatus.ACTIVE);
        String token = loginAndGetToken("student@ue.edu", "Secret123!");

        mockMvc.perform(get("/api/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void teacherCanCreateAndListStudents() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        StudentRegistrationRequest create = new StudentRegistrationRequest();
        create.setEmail("created.student@ue.edu");
        create.setPassword("Secret123!");
        create.setFullName("Created Student");
        create.setDepartmentId(dept.getId());
        create.setPhone("01700000000");
        create.setAddress("Dhaka");


        mockMvc.perform(post("/api/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("created.student@ue.edu"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void get_shouldReturn404_whenStudentNotFound() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        mockMvc.perform(get("/api/students/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found."));
    }

    @Test
    void studentMeEndpoints_shouldWorkForStudentRoleOnly() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenStudent("student@ue.edu", "Secret123!", dept, studentNo(1), StudentStatus.ACTIVE);
        String studentToken = loginAndGetToken("student@ue.edu", "Secret123!");

        mockMvc.perform(get("/api/students/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@ue.edu"));

        StudentUpdateMeRequest updateMe = new StudentUpdateMeRequest();
        updateMe.setPhone("01999");

        mockMvc.perform(put("/api/students/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("01999"));
    }

    @Test
    void update_shouldReturn400_onValidationError() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        Student student = givenStudent("student@ue.edu", "Secret123!", dept, studentNo(1), StudentStatus.ACTIVE);

        StudentUpdateRequest bad = new StudentUpdateRequest();
        bad.setDepartmentId(dept.getId()); // fullName missing => @NotBlank violation

        mockMvc.perform(put("/api/students/" + student.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.details.fieldErrors.fullName").exists());
    }
}
