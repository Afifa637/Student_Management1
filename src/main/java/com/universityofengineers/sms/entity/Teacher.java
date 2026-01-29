package com.universityofengineers.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "teachers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_no", columnNames = "employee_no"),
        @UniqueConstraint(name = "uk_teacher_account", columnNames = "user_account_id")
})
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount account;

    @Column(name = "employee_no", nullable = false, length = 40)
    private String employeeNo;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TeacherTitle title = TeacherTitle.LECTURER;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    // Cardinality: Department 1..M Teacher
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Cardinality: Teacher 1..M Course (a teacher can teach many courses)
    @JsonIgnore
    @OneToMany(mappedBy = "teacher")
    @ToString.Exclude
    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
