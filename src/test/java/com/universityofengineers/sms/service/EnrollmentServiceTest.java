package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.EnrollmentCreateRequest;
import com.universityofengineers.sms.dto.request.GradeUpdateRequest;
import com.universityofengineers.sms.dto.response.EnrollmentResponse;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ForbiddenException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.CourseRepository;
import com.universityofengineers.sms.repository.EnrollmentRepository;
import com.universityofengineers.sms.repository.StudentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
import com.universityofengineers.sms.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks private EnrollmentService enrollmentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enrollMe_shouldReject_whenNotStudentRole() {
        authenticate(Role.TEACHER, 1L, "t@ue.edu");

        EnrollmentCreateRequest req = new EnrollmentCreateRequest();
        req.setCourseId(10L);

        assertThatThrownBy(() -> enrollmentService.enrollMe(req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only students");
    }

    @Test
    void enrollMe_shouldReject_whenStudentNotActive() {
        long accountId = 99L;
        authenticate(Role.STUDENT, accountId, "s@ue.edu");

        Student student = Student.builder()
                .id(1L)
                .status(StudentStatus.SUSPENDED)
                .account(UserAccount.builder().id(accountId).email("s@ue.edu").role(Role.STUDENT).enabled(true).passwordHash("h").build())
                .department(Department.builder().id(1L).code("CSE").name("CSE").build())
                .studentNo("UE-2026-000001")
                .fullName("Student")
                .build();

        when(studentRepository.findByAccountId(accountId)).thenReturn(Optional.of(student));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        EnrollmentCreateRequest req = new EnrollmentCreateRequest();
        req.setCourseId(10L);

        assertThatThrownBy(() -> enrollmentService.enrollMe(req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only ACTIVE students");
    }

    @Test
    void enrollMe_shouldReject_whenAlreadyEnrolled() {
        long accountId = 99L;
        authenticate(Role.STUDENT, accountId, "s@ue.edu");


        Department dept = Department.builder().id(1L).code("CSE").name("CSE").build();
        Student student = Student.builder()
                .id(1L)
                .status(StudentStatus.ACTIVE)
                .account(UserAccount.builder().id(accountId).email("s@ue.edu").role(Role.STUDENT).enabled(true).passwordHash("h").build())
                .department(dept)
                .studentNo("UE-2026-000001")
                .fullName("Student")
                .build();

        Course course = Course.builder()
                .id(10L)
                .department(dept)
                .teacher(Teacher.builder().id(7L).department(dept)
                        .account(UserAccount.builder().id(50L).email("t@ue.edu").role(Role.TEACHER).enabled(true).passwordHash("h").build())
                        .employeeNo("UE-T-000007").fullName("Teacher").title(TeacherTitle.PROFESSOR).build())
                .code("CSE101").title("Intro").credit(3.0).capacity(2)
                .build();

        Enrollment existing = Enrollment.builder().id(500L).student(student).course(course).status(EnrollmentStatus.ENROLLED).build();

        when(studentRepository.findByAccountId(accountId)).thenReturn(Optional.of(student));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(existing));

        EnrollmentCreateRequest req = new EnrollmentCreateRequest();
        req.setCourseId(10L);

        assertThatThrownBy(() -> enrollmentService.enrollMe(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Already enrolled");
    }

    @Test
    void setGrade_shouldUppercaseGrade_andMarkCompleted_whenEnrolled() {
        long teacherAccountId = 50L;
        authenticate(Role.TEACHER, teacherAccountId, "t@ue.edu");

        Department dept = Department.builder().id(1L).code("CSE").name("CSE").build();
        Teacher currentTeacher = Teacher.builder()
                .id(7L)
                .account(UserAccount.builder().id(teacherAccountId).email("t@ue.edu").role(Role.TEACHER).enabled(true).passwordHash("h").build())
                .employeeNo("UE-T-000007")
                .fullName("Teacher")
                .title(TeacherTitle.PROFESSOR)
                .department(dept)
                .build();

        Course course = Course.builder()
                .id(10L)
                .department(dept)
                .teacher(currentTeacher)
                .code("CSE101").title("Intro").credit(3.0).capacity(2)
                .build();

        Student student = Student.builder()
                .id(1L)
                .department(dept)
                .account(UserAccount.builder().id(99L).email("s@ue.edu").role(Role.STUDENT).enabled(true).passwordHash("h").build())
                .studentNo("UE-2026-000001")
                .fullName("Student")
                .status(StudentStatus.ACTIVE)
                .build();

        Enrollment enrollment = Enrollment.builder()
                .id(123L)
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        when(enrollmentRepository.findById(123L)).thenReturn(Optional.of(enrollment));
        when(teacherRepository.findByAccountId(teacherAccountId)).thenReturn(Optional.of(currentTeacher));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeUpdateRequest req = new GradeUpdateRequest();
        req.setGrade(" a- ");

        EnrollmentResponse res = enrollmentService.setGrade(123L, req);

        assertThat(res.getGrade()).isEqualTo("A-");
        assertThat(res.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }


    @Test
    void myEnrollments_shouldThrow_whenCurrentStudentProfileMissing() {
        long accountId = 99L;
        authenticate(Role.STUDENT, accountId, "s@ue.edu");
        when(studentRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.myEnrollments())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student profile not found");
    }

    private void authenticate(Role role, long accountId, String email) {
        UserAccount account = UserAccount.builder()
                .id(accountId)
                .email(email)
                .passwordHash("hash")
                .role(role)
                .enabled(true)
                .build();
        UserPrincipal principal = new UserPrincipal(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
