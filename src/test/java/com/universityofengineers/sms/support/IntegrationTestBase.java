package com.universityofengineers.sms.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universityofengineers.sms.dto.request.LoginRequest;
import com.universityofengineers.sms.dto.response.AuthResponse;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Year;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected PasswordEncoder passwordEncoder;

    @Autowired protected DepartmentRepository departmentRepository;
    @Autowired protected UserAccountRepository userAccountRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected CourseRepository courseRepository;
    @Autowired protected EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void cleanDatabase() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        userAccountRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    protected Department givenDepartment(String code, String name) {
        return departmentRepository.save(Department.builder().code(code).name(name).build());
    }

    protected Teacher givenTeacher(String email, String rawPassword, Department dept, String employeeNo, TeacherTitle title) {
        UserAccount ua = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.TEACHER)
                .enabled(true)
                .build());

        return teacherRepository.save(Teacher.builder()
                .account(ua)
                .employeeNo(employeeNo)
                .fullName("Test Teacher")
                .title(title)
                .department(dept)
                .build());
    }

    protected Student givenStudent(String email, String rawPassword, Department dept, String studentNo, StudentStatus status) {
        UserAccount ua = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        return studentRepository.save(Student.builder()
                .account(ua)
                .studentNo(studentNo)
                .fullName("Test Student")
                .phone("01700000000")
                .address("Dhaka")
                .status(status)
                .department(dept)
                .build());
    }

    protected Course givenCourse(String code, Department dept, Teacher teacher, int capacity) {
        return courseRepository.save(Course.builder()
                .code(code)
                .title("Test Course")
                .credit(3.0)
                .capacity(capacity)
                .department(dept)
                .teacher(teacher)
                .build());
    }

    protected String loginAndGetToken(String email, String rawPassword) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(rawPassword);


        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsByteArray(), AuthResponse.class);
        return auth.getToken();
    }

    protected static String studentNo(int n) {
        return "UE-" + Year.now().getValue() + "-" + String.format("%06d", n);
    }

    protected HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }
}
