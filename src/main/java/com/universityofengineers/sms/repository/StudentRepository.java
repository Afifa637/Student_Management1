package com.universityofengineers.sms.repository;

import com.universityofengineers.sms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByAccountId(Long accountId);
    boolean existsByStudentNo(String studentNo);
}
