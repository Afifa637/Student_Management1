package com.universityofengineers.sms.config;

import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.repository.CourseRepository;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
import com.universityofengineers.sms.repository.UserAccountRepository;
import com.universityofengineers.sms.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BootstrapDataConfig {

    private final DepartmentRepository departmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.teacher.email:admin.teacher@ue.edu}")
    private String bootstrapTeacherEmail;

    @Value("${app.bootstrap.teacher.password:ChangeMe123!}")
    private String bootstrapTeacherPassword;

    @Bean
    CommandLineRunner bootstrapData() {
        return args -> {
            if (!bootstrapEnabled) return;

            // Departments (example: University of Engineers)
            if (departmentRepository.count() == 0) {
                List<Department> depts = List.of(
                        Department.builder().code("CSE").name("Computer Science & Engineering").build(),
                        Department.builder().code("EEE").name("Electrical & Electronic Engineering").build(),
                        Department.builder().code("ME").name("Mechanical Engineering").build(),
                        Department.builder().code("CE").name("Civil Engineering").build()
                );
                departmentRepository.saveAll(depts);
            }

            // Create a first TEACHER account if none exists.
            if (teacherRepository.count() == 0) {
                Department dept = departmentRepository.findByCode("CSE").orElseGet(() -> departmentRepository.findAll().get(0));

                String email = bootstrapTeacherEmail.trim().toLowerCase();
                if (!userAccountRepository.existsByEmail(email)) {
                    UserAccount account = UserAccount.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode(bootstrapTeacherPassword))
                            .role(Role.TEACHER)
                            .enabled(true)
                            .build();
                    account = userAccountRepository.save(account);

                    String employeeNo = CodeGenerator.newEmployeeNo();
                    Teacher teacher = Teacher.builder()
                            .account(account)
                            .employeeNo(employeeNo)
                            .fullName("Admin Teacher")
                            .title(TeacherTitle.PROFESSOR)
                            .hireDate(LocalDate.now().minusYears(5))
                            .department(dept)
                            .build();
                    teacher = teacherRepository.save(teacher);

                    // Some sample courses
                    if (courseRepository.count() == 0) {
                        courseRepository.saveAll(List.of(
                                Course.builder().code("CSE101").title("Introduction to Programming").credit(3.0).capacity(80).department(dept).teacher(teacher).build(),
                                Course.builder().code("CSE220").title("Data Structures").credit(3.0).capacity(70).department(dept).teacher(teacher).build()
                        ));
                    }
                }
            }
        };
    }
}
