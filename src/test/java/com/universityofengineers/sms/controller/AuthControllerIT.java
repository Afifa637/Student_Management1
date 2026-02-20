package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.LoginRequest;
import com.universityofengineers.sms.dto.request.StudentRegistrationRequest;
import com.universityofengineers.sms.entity.Department;
import com.universityofengineers.sms.entity.Role;
import com.universityofengineers.sms.entity.TeacherTitle;
import com.universityofengineers.sms.support.IntegrationTestBase;
import com.universityofengineers.sms.support.SmsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SmsIntegrationTest
class AuthControllerIT extends IntegrationTestBase {

    @Test
    void registerStudent_shouldReturn201_andJwtTokenAndStudentId() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");

        StudentRegistrationRequest req = new StudentRegistrationRequest();
        req.setEmail("new.student@ue.edu");
        req.setPassword("Secret123!");
        req.setFullName("New Student");
        req.setDepartmentId(dept.getId());
        req.setPhone("01700000000");
        req.setAddress("Dhaka");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("new.student@ue.edu"))
                .andExpect(jsonPath("$.studentId").isNumber())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void registerStudent_shouldReturn400_onValidationError() throws Exception {
        StudentRegistrationRequest req = new StudentRegistrationRequest();
        req.setFullName("X");
        req.setDepartmentId(1L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.details.fieldErrors.email").exists())
                .andExpect(jsonPath("$.details.fieldErrors.password").exists());
    }

    @Test
    void login_shouldReturn200_forValidCredentials() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);


        LoginRequest req = new LoginRequest();
        req.setEmail("teacher@ue.edu");
        req.setPassword("Secret123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value(Role.TEACHER.name()))
                .andExpect(jsonPath("$.teacherId").isNumber());
    }

    @Test
    void login_shouldReturn401_forBadCredentials() throws Exception {
        Department dept = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", dept, "UE-T-000001", TeacherTitle.PROFESSOR);

        LoginRequest req = new LoginRequest();
        req.setEmail("teacher@ue.edu");
        req.setPassword("WRONG_PASSWORD");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }
}
