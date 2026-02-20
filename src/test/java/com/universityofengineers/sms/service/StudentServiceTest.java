package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.PasswordResetRequest;
import com.universityofengineers.sms.dto.request.StudentRegistrationRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateMeRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateRequest;
import com.universityofengineers.sms.dto.response.StudentResponse;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.StudentRepository;
import com.universityofengineers.sms.repository.UserAccountRepository;
import com.universityofengineers.sms.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private StudentService studentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createByTeacher_shouldTrimLowercaseEmail_generateStudentNo_andPersist() {
        Department dept = Department.builder().id(1L).code("CSE").name("Computer Science").build();


        StudentRegistrationRequest req = new StudentRegistrationRequest();
        req.setEmail("  Student@Example.com  ");
        req.setPassword("Secret123!");
        req.setFullName("  Alice  ");
        req.setPhone("  01234  ");
        req.setAddress(null);
        req.setDepartmentId(1L);

        when(userAccountRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed");

        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount ua = inv.getArgument(0);
            ua.setId(100L);
            return ua;
        });
        when(studentRepository.existsByStudentNo(anyString())).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(200L);
            return s;
        });

        StudentResponse res = studentService.createByTeacher(req);

        assertThat(res.getId()).isEqualTo(200L);
        assertThat(res.getEmail()).isEqualTo("student@example.com");
        assertThat(res.getFullName()).isEqualTo("Alice");
        assertThat(res.getPhone()).isEqualTo("01234");
        assertThat(res.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(res.getStudentNo()).startsWith("UE-" + Year.now().getValue() + "-");

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(accountCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void createByTeacher_shouldRejectWhenEmailAlreadyExists() {
        StudentRegistrationRequest req = new StudentRegistrationRequest();
        req.setEmail("dup@ue.edu");
        req.setPassword("Secret123!");
        req.setFullName("Alice");
        req.setDepartmentId(1L);

        when(userAccountRepository.existsByEmail("dup@ue.edu")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createByTeacher(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");

        verify(userAccountRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createByTeacher_shouldFailAfterTooManyStudentNoCollisions() {
        Department dept = Department.builder().id(1L).code("CSE").name("CSE").build();

        StudentRegistrationRequest req = new StudentRegistrationRequest();
        req.setEmail("s@ue.edu");
        req.setPassword("Secret123!");
        req.setFullName("Alice");
        req.setDepartmentId(1L);

        when(userAccountRepository.existsByEmail("s@ue.edu")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount ua = inv.getArgument(0);
            ua.setId(1L);
            return ua;
        });

        when(studentRepository.existsByStudentNo(anyString())).thenReturn(true);

        assertThatThrownBy(() -> studentService.createByTeacher(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not generate unique student number");

        verify(studentRepository, never()).save(any());
    }


    @Test
    void update_shouldTrimOptionalFields_andPersist() {
        Department dept = Department.builder().id(1L).code("CSE").name("CSE").build();
        Student student = Student.builder()
                .id(10L)
                .studentNo("UE-2026-000001")
                .fullName("Old")
                .department(dept)
                .account(UserAccount.builder().id(99L).email("s@ue.edu").role(Role.STUDENT).enabled(true).passwordHash("hash").build())
                .status(StudentStatus.ACTIVE)
                .build();

        StudentUpdateRequest req = new StudentUpdateRequest();
        req.setFullName("  New Name ");
        req.setDepartmentId(1L);
        req.setPhone("  0123 ");
        req.setAddress("  Dhaka ");

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse res = studentService.update(10L, req);

        assertThat(res.getFullName()).isEqualTo("New Name");
        assertThat(res.getPhone()).isEqualTo("0123");
        assertThat(res.getAddress()).isEqualTo("Dhaka");
        verify(studentRepository).save(student);
    }

    @Test
    void disableStudent_shouldDisableLogin_andSoftDropIfActive() {
        UserAccount account = UserAccount.builder().id(1L).email("s@ue.edu").passwordHash("h").role(Role.STUDENT).enabled(true).build();
        Student student = Student.builder()
                .id(10L)
                .status(StudentStatus.ACTIVE)
                .account(account)
                .department(Department.builder().id(1L).code("CSE").name("CSE").build())
                .studentNo("UE-2026-000001")
                .fullName("Alice")
                .build();

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        studentService.disableStudent(10L);

        assertThat(account.isEnabled()).isFalse();
        assertThat(student.getStatus()).isEqualTo(StudentStatus.DROPPED);
    }

    @Test
    void resetStudentPassword_shouldRejectWhenAccountDisabled() {
        UserAccount account = UserAccount.builder().id(1L).email("s@ue.edu").passwordHash("h").role(Role.STUDENT).enabled(false).build();
        Student student = Student.builder()
                .id(10L)
                .account(account)
                .department(Department.builder().id(1L).code("CSE").name("CSE").build())
                .studentNo("UE-2026-000001")
                .fullName("Alice")
                .status(StudentStatus.ACTIVE)
                .build();

        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));

        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("NewPass123!");

        assertThatThrownBy(() -> studentService.resetStudentPassword(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account is disabled");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void updateMe_shouldUseAuthenticatedAccountId_andPersistOnlyProvidedFields() {
        UserAccount account = UserAccount.builder().id(777L).email("me@ue.edu").passwordHash("h").role(Role.STUDENT).enabled(true).build();
        UserPrincipal principal = new UserPrincipal(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );


        Student student = Student.builder()
                .id(10L)
                .account(account)
                .studentNo("UE-2026-000777")
                .fullName("Me")
                .department(Department.builder().id(1L).code("CSE").name("CSE").build())
                .status(StudentStatus.ACTIVE)
                .phone("old")
                .address("old")
                .build();

        when(studentRepository.findByAccountId(777L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentUpdateMeRequest req = new StudentUpdateMeRequest();
        req.setPhone("  01999 ");

        StudentResponse res = studentService.updateMe(req);

        assertThat(res.getPhone()).isEqualTo("01999");
        assertThat(res.getAddress()).isEqualTo("old");
    }
}
