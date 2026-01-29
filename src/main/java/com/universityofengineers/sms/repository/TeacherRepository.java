package com.universityofengineers.sms.repository;

import com.universityofengineers.sms.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByAccountId(Long accountId);
    boolean existsByEmployeeNo(String employeeNo);
}
