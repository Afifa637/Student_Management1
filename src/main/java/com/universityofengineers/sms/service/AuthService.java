package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.LoginRequest;
import com.universityofengineers.sms.dto.request.StudentRegistrationRequest;
import com.universityofengineers.sms.dto.response.AuthResponse;
import com.universityofengineers.sms.entity.Role;
import com.universityofengineers.sms.entity.Student;
import com.universityofengineers.sms.entity.StudentStatus;
import com.universityofengineers.sms.entity.UserAccount;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.StudentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
import com.universityofengineers.sms.repository.UserAccountRepository;
import com.universityofengineers.sms.security.JwtService;
import com.universityofengineers.sms.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Mature sign-up rule:
     * - Public registration is ONLY for STUDENT.
     * - The API does not accept a "role" field at all.
     * - Teacher accounts can only be created by an already-authenticated teacher.
     */
    @Transactional
    public AuthResponse registerStudent(StudentRegistrationRequest req) {
        if (userAccountRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered.");
        }

        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        UserAccount account = UserAccount.builder()
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.STUDENT)
                .enabled(true)
                .build();
        account = userAccountRepository.save(account);

        String studentNo;
        int attempts = 0;
        do {
            if (attempts++ > 10) throw new BadRequestException("Could not generate unique student number. Try again.");
            studentNo = CodeGenerator.newStudentNo();
        } while (studentRepository.existsByStudentNo(studentNo));

        Student student = Student.builder()
                .account(account)
                .studentNo(studentNo)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .address(req.getAddress())
                .status(StudentStatus.ACTIVE)
                .department(dept)
                .build();

        student = studentRepository.save(student);

        String token = jwtService.generateToken(account.getId(), account.getEmail(), account.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMillis(jwtService.getExpirationMillis())
                .role(account.getRole())
                .accountId(account.getId())
                .email(account.getEmail())
                .studentId(student.getId())
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail().toLowerCase(), req.getPassword())
        );

        UserAccount account = userAccountRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (!account.isEnabled()) {
            throw new BadRequestException("Account is disabled.");
        }

        String token = jwtService.generateToken(account.getId(), account.getEmail(), account.getRole());

        Long studentId = studentRepository.findByAccountId(account.getId()).map(Student::getId).orElse(null);
        Long teacherId = teacherRepository.findByAccountId(account.getId()).map(t -> t.getId()).orElse(null);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMillis(jwtService.getExpirationMillis())
                .role(account.getRole())
                .accountId(account.getId())
                .email(account.getEmail())
                .studentId(studentId)
                .teacherId(teacherId)
                .build();
    }
}
