package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.PasswordResetRequest;
import com.universityofengineers.sms.dto.request.StudentRegistrationRequest;
import com.universityofengineers.sms.dto.request.StudentStatusUpdateRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateMeRequest;
import com.universityofengineers.sms.dto.request.StudentUpdateRequest;
import com.universityofengineers.sms.dto.response.DepartmentResponse;
import com.universityofengineers.sms.dto.response.StudentResponse;
import com.universityofengineers.sms.entity.Role;
import com.universityofengineers.sms.entity.Student;
import com.universityofengineers.sms.entity.StudentStatus;
import com.universityofengineers.sms.entity.UserAccount;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.StudentRepository;
import com.universityofengineers.sms.repository.UserAccountRepository;
import com.universityofengineers.sms.util.CodeGenerator;
import com.universityofengineers.sms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    // -----------------------------
    // TEACHER: CRUD
    // -----------------------------

    @Transactional
    public StudentResponse createByTeacher(StudentRegistrationRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userAccountRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered.");
        }

        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        UserAccount account = UserAccount.builder()
                .email(email)
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
                .fullName(req.getFullName().trim())
                .phone(req.getPhone() == null ? null : req.getPhone().trim())
                .address(req.getAddress() == null ? null : req.getAddress().trim())
                .department(dept)
                .status(StudentStatus.ACTIVE)
                .build();

        return toResponse(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> list() {
        return studentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        return toResponse(studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found.")));
    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest req) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));

        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        s.setFullName(req.getFullName().trim());
        s.setDepartment(dept);

        // Optional fields
        s.setPhone(req.getPhone() == null ? null : req.getPhone().trim());
        s.setAddress(req.getAddress() == null ? null : req.getAddress().trim());
        s.setDateOfBirth(req.getDateOfBirth());

        return toResponse(studentRepository.save(s));
    }

    @Transactional
    public StudentResponse updateStatus(Long studentId, StudentStatusUpdateRequest req) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        s.setStatus(req.getStatus());
        return toResponse(studentRepository.save(s));
    }

    /**
     * Soft delete:
     * - disable login
     * - mark DROPPED if active
     */
    @Transactional
    public void disableStudent(Long studentId) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        UserAccount account = s.getAccount();
        account.setEnabled(false);
        userAccountRepository.save(account);

        if (s.getStatus() == StudentStatus.ACTIVE) {
            s.setStatus(StudentStatus.DROPPED);
            studentRepository.save(s);
        }
    }

    @Transactional
    public void resetStudentPassword(Long studentId, PasswordResetRequest req) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        UserAccount account = s.getAccount();
        if (!account.isEnabled()) throw new BadRequestException("Account is disabled.");
        account.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userAccountRepository.save(account);
    }

    // -----------------------------
    // STUDENT: self-service
    // -----------------------------

    public Student getCurrentStudentEntity() {
        Long accountId = SecurityUtils.currentAccountId();
        return studentRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));
    }

    @Transactional(readOnly = true)
    public StudentResponse me() {
        return toResponse(getCurrentStudentEntity());
    }

    @Transactional
    public StudentResponse updateMe(StudentUpdateMeRequest req) {
        Student s = getCurrentStudentEntity();
        if (req.getPhone() != null) s.setPhone(req.getPhone().trim());
        if (req.getAddress() != null) s.setAddress(req.getAddress().trim());
        return toResponse(studentRepository.save(s));
    }

    private StudentResponse toResponse(Student s) {
        DepartmentResponse dept = DepartmentResponse.builder()
                .id(s.getDepartment().getId())
                .code(s.getDepartment().getCode())
                .name(s.getDepartment().getName())
                .build();

        return StudentResponse.builder()
                .id(s.getId())
                .studentNo(s.getStudentNo())
                .fullName(s.getFullName())
                .email(s.getAccount().getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .status(s.getStatus())
                .department(dept)
                .build();
    }
}
