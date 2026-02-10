package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.CourseUpsertRequest;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.support.IntegrationTestBase;
import com.universityofengineers.sms.support.SmsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SmsIntegrationTest
class CourseControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void list_shouldReturnCoursesOnlyFromCallerDepartment() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        Department eee = givenDepartment("EEE", "Electrical");

        Teacher teacherCse = givenTeacher("teacher.cse@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        Teacher teacherEee = givenTeacher("teacher.eee@ue.edu", "Secret123!", eee, "UE-T-000002", TeacherTitle.LECTURER);

        givenCourse("CSE101", cse, teacherCse, 60);
        givenCourse("EEE101", eee, teacherEee, 60);

        String teacherToken = loginAndGetToken("teacher.cse@ue.edu", "Secret123!");
        mockMvc.perform(get("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].department.code").value("CSE"))
                .andExpect(jsonPath("$[0].code").value("CSE101"));

        givenStudent("student@ue.edu", "Secret123!", eee, studentNo(1), StudentStatus.ACTIVE);
        String studentToken = loginAndGetToken("student@ue.edu", "Secret123!");
        mockMvc.perform(get("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].department.code").value("EEE"))
                .andExpect(jsonPath("$[0].code").value("EEE101"));
    }

    @Test
    void create_shouldRequireTeacherRole_andRejectStudent() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        givenStudent("student@ue.edu", "Secret123!", cse, studentNo(1), StudentStatus.ACTIVE);

        String studentToken = loginAndGetToken("student@ue.edu", "Secret123!");

        CourseUpsertRequest req = new CourseUpsertRequest();
        req.setCode("CSE999");
        req.setTitle("Forbidden Course");
        req.setCredit(3.0);
        req.setCapacity(10);
        req.setDepartmentId(cse.getId());

        mockMvc.perform(post("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }


    @Test
    void teacherCannotCreateCourseInAnotherDepartment_shouldReturn403() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        Department eee = givenDepartment("EEE", "Electrical");
        givenTeacher("teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        CourseUpsertRequest req = new CourseUpsertRequest();
        req.setCode("EEE999");
        req.setTitle("Cross Dept Course");
        req.setCredit(3.0);
        req.setCapacity(10);
        req.setDepartmentId(eee.getId());

        mockMvc.perform(post("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only create courses within your own department."));
    }

    @Test
    void get_shouldReturn404_whenCourseNotFound() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        givenTeacher("teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");

        mockMvc.perform(get("/api/courses/9999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found."));
    }

    @Test
    void protectedEndpoint_shouldReturn401_forInvalidJwt() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
