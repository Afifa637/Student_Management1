package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.TeacherCreateRequest;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.support.IntegrationTestBase;
import com.universityofengineers.sms.support.SmsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SmsIntegrationTest
class TeacherControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void studentCannotAccessTeacherEndpoints() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenStudent("student@ue.edu", "Secret123!", dept, studentNo(1), StudentStatus.ACTIVE);
        String token = loginAndGetToken("student@ue.edu", "Secret123!");

        mockMvc.perform(get("/api/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void teacherCanCreateAnotherTeacher_andListTeachers() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        Department dept2 = givenDepartment("EEE", "Electrical");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        TeacherCreateRequest req = new TeacherCreateRequest();
        req.setEmail("new.teacher@ue.edu");
        req.setPassword("Secret123!");
        req.setFullName("New Teacher");
        req.setDepartmentId(dept2.getId());
        req.setTitle(TeacherTitle.LECTURER);

        mockMvc.perform(post("/api/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.teacher@ue.edu"))
                .andExpect(jsonPath("$.department.code").value("EEE"));

        mockMvc.perform(get("/api/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void teacherCannotDisableSelf_shouldReturn400() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        Teacher me = givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        mockMvc.perform(delete("/api/teachers/" + me.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot disable your own teacher account."));
    }

    @Test
    void setEnabled_shouldToggleTeacherEnabledFlag() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);
        Teacher other = givenTeacher("other.teacher@ue.edu", "Secret123!", dept, "UE-T-000002", TeacherTitle.LECTURER);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");


        mockMvc.perform(put("/api/teachers/" + other.getId() + "/enabled")
                        .queryParam("enabled", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Teacher enabled status updated."));
    }
}
