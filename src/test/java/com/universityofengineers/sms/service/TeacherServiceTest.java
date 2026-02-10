package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.PasswordResetRequest;
import com.universityofengineers.sms.dto.request.TeacherCreateRequest;
import com.universityofengineers.sms.dto.request.TeacherUpdateMeRequest;
import com.universityofengineers.sms.dto.response.TeacherResponse;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
import com.universityofengineers.sms.repository.UserAccountRepository;
import com.universityofengineers.sms.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock private TeacherRepository teacherRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private TeacherService teacherService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldLowercaseEmail_generateEmployeeNo_andPersist() {
        Department dept = Department.builder().id(1L).code("CSE").name("CSE").build();

        TeacherCreateRequest req = new TeacherCreateRequest();
        req.setEmail("  TEACHER@UE.EDU ");
        req.setPassword("Secret123!");
        req.setFullName("  Dr. Ada ");
        req.setDepartmentId(1L);
        req.setTitle(TeacherTitle.PROFESSOR);
        req.setHireDate(LocalDate.of(2020, 1, 1));

        when(userAccountRepository.existsByEmail("teacher@ue.edu")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");

        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount ua = inv.getArgument(0);
            ua.setId(100L);
            return ua;
        });
        when(teacherRepository.existsByEmployeeNo(anyString())).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> {
            Teacher t = inv.getArgument(0);
            t.setId(200L);
            return t;
        });

        TeacherResponse res = teacherService.create(req);


        assertThat(res.getId()).isEqualTo(200L);
        assertThat(res.getEmail()).isEqualTo("teacher@ue.edu");
        assertThat(res.getFullName()).isEqualTo("Dr. Ada");
        assertThat(res.getTitle()).isEqualTo(TeacherTitle.PROFESSOR);
        assertThat(res.getEmployeeNo()).startsWith("UE-T-");
    }

    @Test
    void create_shouldRejectDuplicateEmail() {
        TeacherCreateRequest req = new TeacherCreateRequest();
        req.setEmail("t@ue.edu");
        req.setPassword("Secret123!");
        req.setFullName("Teacher");
        req.setDepartmentId(1L);
        req.setTitle(TeacherTitle.LECTURER);

        when(userAccountRepository.existsByEmail("t@ue.edu")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void me_shouldThrow_whenTeacherProfileMissing() {
        UserAccount account = UserAccount.builder().id(777L).email("me@ue.edu").passwordHash("h").role(Role.TEACHER).enabled(true).build();
        UserPrincipal principal = new UserPrincipal(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(teacherRepository.findByAccountId(777L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.me())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Teacher profile not found");
    }

    @Test
    void resetTeacherPassword_shouldRejectWhenAccountDisabled() {
        UserAccount account = UserAccount.builder().id(1L).email("t@ue.edu").passwordHash("h").role(Role.TEACHER).enabled(false).build();
        Teacher teacher = Teacher.builder()
                .id(10L)
                .account(account)
                .employeeNo("UE-T-000001")
                .fullName("Teacher")
                .title(TeacherTitle.LECTURER)
                .department(Department.builder().id(1L).code("CSE").name("CSE").build())
                .build();

        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));

        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("NewPass123!");

        assertThatThrownBy(() -> teacherService.resetTeacherPassword(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account is disabled");
    }

    @Test
    void updateMe_shouldAllowPartialUpdate_forAuthenticatedTeacher() {
        UserAccount account = UserAccount.builder().id(777L).email("me@ue.edu").passwordHash("h").role(Role.TEACHER).enabled(true).build();
        UserPrincipal principal = new UserPrincipal(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        Department oldDept = Department.builder().id(1L).code("CSE").name("CSE").build();
        Department newDept = Department.builder().id(2L).code("EEE").name("EEE").build();

        Teacher teacher = Teacher.builder()
                .id(10L)
                .account(account)
                .employeeNo("UE-T-000777")
                .fullName("Teacher")
                .title(TeacherTitle.LECTURER)
                .hireDate(LocalDate.of(2020, 1, 1))
                .department(oldDept)
                .build();

        when(teacherRepository.findByAccountId(777L)).thenReturn(Optional.of(teacher));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherUpdateMeRequest req = new TeacherUpdateMeRequest();
        req.setTitle(TeacherTitle.ASSOCIATE_PROFESSOR);
        req.setDepartmentId(2L);

        TeacherResponse res = teacherService.updateMe(req);


        assertThat(res.getTitle()).isEqualTo(TeacherTitle.ASSOCIATE_PROFESSOR);
        assertThat(res.getDepartment().getCode()).isEqualTo("EEE");
    }
}
