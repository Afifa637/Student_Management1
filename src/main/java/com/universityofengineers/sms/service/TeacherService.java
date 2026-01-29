package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.TeacherCreateRequest;
import com.universityofengineers.sms.dto.request.TeacherUpdateRequest;
import com.universityofengineers.sms.dto.response.DepartmentResponse;
import com.universityofengineers.sms.dto.response.TeacherResponse;
import com.universityofengineers.sms.entity.Role;
import com.universityofengineers.sms.entity.Teacher;
import com.universityofengineers.sms.entity.UserAccount;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
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
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserAccountRepository userAccountRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TeacherResponse> list() {
        return teacherRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(Long id) {
        return toResponse(teacherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher not found.")));
    }

    public Teacher getCurrentTeacherEntity() {
        Long accountId = SecurityUtils.currentAccountId();
        return teacherRepository.findByAccountId(accountId).orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found."));
    }

    @Transactional(readOnly = true)
    public TeacherResponse me() {
        return toResponse(getCurrentTeacherEntity());
    }

    @Transactional
    public TeacherResponse create(TeacherCreateRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userAccountRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered.");
        }

        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        UserAccount account = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.TEACHER)
                .enabled(true)
                .build();
        account = userAccountRepository.save(account);

        String employeeNo;
        int attempts = 0;
        do {
            if (attempts++ > 10) throw new BadRequestException("Could not generate unique employee number. Try again.");
            employeeNo = CodeGenerator.newEmployeeNo();
        } while (teacherRepository.existsByEmployeeNo(employeeNo));

        Teacher teacher = Teacher.builder()
                .account(account)
                .employeeNo(employeeNo)
                .fullName(req.getFullName().trim())
                .department(dept)
                .title(req.getTitle())
                .hireDate(req.getHireDate())
                .build();

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherUpdateRequest req) {
        Teacher t = teacherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher not found."));
        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        t.setFullName(req.getFullName().trim());
        t.setTitle(req.getTitle());
        t.setHireDate(req.getHireDate());
        t.setDepartment(dept);
        return toResponse(teacherRepository.save(t));
    }

    @Transactional
    public void disableTeacher(Long id) {
        Teacher t = teacherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher not found."));
        UserAccount account = t.getAccount();
        account.setEnabled(false);
        userAccountRepository.save(account);
    }

    private TeacherResponse toResponse(Teacher t) {
        DepartmentResponse dept = DepartmentResponse.builder()
                .id(t.getDepartment().getId())
                .code(t.getDepartment().getCode())
                .name(t.getDepartment().getName())
                .build();

        return TeacherResponse.builder()
                .id(t.getId())
                .employeeNo(t.getEmployeeNo())
                .fullName(t.getFullName())
                .email(t.getAccount().getEmail())
                .title(t.getTitle())
                .department(dept)
                .build();
    }
}