package com.universityofengineers.sms.controller;

import com.universityofengineers.sms.dto.request.EnrollmentCreateRequest;
import com.universityofengineers.sms.dto.request.GradeUpdateRequest;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.support.IntegrationTestBase;
import com.universityofengineers.sms.support.SmsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SmsIntegrationTest
class EnrollmentControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void studentEnrollFlow_shouldCoverEnroll_list_drop_andTeacherGrade() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        Teacher teacher = givenTeacher("teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        Student student = givenStudent("student@ue.edu", "Secret123!", cse, studentNo(1), StudentStatus.ACTIVE);
        Course course = givenCourse("CSE101", cse, teacher, 2);

        String teacherToken = loginAndGetToken("teacher@ue.edu", "Secret123!");
        String studentToken = loginAndGetToken("student@ue.edu", "Secret123!");

        EnrollmentCreateRequest enrollReq = new EnrollmentCreateRequest();
        enrollReq.setCourseId(course.getId());

        MvcResult enrollResult = mockMvc.perform(post("/api/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENROLLED"))
                .andReturn();

        long enrollmentId = objectMapper.readTree(enrollResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(get("/api/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value(student.getId()));

        GradeUpdateRequest gradeReq = new GradeUpdateRequest();
        gradeReq.setGrade("a-");

        mockMvc.perform(put("/api/enrollments/" + enrollmentId + "/grade")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("A-"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/enrollments/me/" + enrollmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enrollment is not active."));
    }


    @Test
    void studentCanDropActiveEnrollment_shouldReturn200() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        Teacher teacher = givenTeacher("teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        givenCourse("CSE101", cse, teacher, 2);

        givenStudent("student@ue.edu", "Secret123!", cse, studentNo(1), StudentStatus.ACTIVE);
        String studentToken = loginAndGetToken("student@ue.edu", "Secret123!");

        EnrollmentCreateRequest enrollReq = new EnrollmentCreateRequest();
        enrollReq.setCourseId(courseRepository.findByCode("CSE101").orElseThrow().getId());

        MvcResult enrollResult = mockMvc.perform(post("/api/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollReq)))
                .andExpect(status().isOk())
                .andReturn();

        long enrollmentId = objectMapper.readTree(enrollResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/enrollments/me/" + enrollmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Enrollment dropped."));
    }

    @Test
    void studentEnroll_shouldReturn400_whenEnrollSameCourseTwice() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        Teacher teacher = givenTeacher("teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        givenCourse("CSE101", cse, teacher, 2);

        givenStudent("student@ue.edu", "Secret123!", cse, studentNo(1), StudentStatus.ACTIVE);
        String studentToken = loginAndGetToken("student@ue.edu", "Secret123!");

        EnrollmentCreateRequest req = new EnrollmentCreateRequest();
        req.setCourseId(courseRepository.findByCode("CSE101").orElseThrow().getId());

        mockMvc.perform(post("/api/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/enrollments/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Already enrolled in this course."));
    }

    @Test
    void teacherEnrollStudent_shouldReturn403_whenNotCourseTeacher() throws Exception {
        Department cse = givenDepartment("CSE", "Computer Science");
        Teacher courseTeacher = givenTeacher("course.teacher@ue.edu", "Secret123!", cse, "UE-T-000001", TeacherTitle.PROFESSOR);
        Teacher otherTeacher = givenTeacher("other.teacher@ue.edu", "Secret123!", cse, "UE-T-000002", TeacherTitle.LECTURER);

        Student student = givenStudent("student@ue.edu", "Secret123!", cse, studentNo(1), StudentStatus.ACTIVE);
        Course course = givenCourse("CSE101", cse, courseTeacher, 2);

        String otherTeacherToken = loginAndGetToken("other.teacher@ue.edu", "Secret123!");

        EnrollmentCreateRequest enroll = new EnrollmentCreateRequest();
        enroll.setCourseId(course.getId());


        mockMvc.perform(post("/api/students/" + student.getId() + "/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enroll)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only enroll students into your own courses."));
    }

    @Test
    void endpointsShouldReturn401_whenNoJwtProvided() throws Exception {
        mockMvc.perform(get("/api/enrollments/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/enrollments"))
                .andExpect(status().isUnauthorized());
    }
}
