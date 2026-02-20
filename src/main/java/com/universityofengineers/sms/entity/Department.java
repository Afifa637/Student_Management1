package com.universityofengineers.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_code", columnNames = "code")
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Cardinality: Department 1..M Student
    @JsonIgnore
    @OneToMany(mappedBy = "department")
    @ToString.Exclude
    @Builder.Default
    private List<Student> students = new ArrayList<>();

    // Cardinality: Department 1..M Teacher
    @JsonIgnore
    @OneToMany(mappedBy = "department")
    @ToString.Exclude
    @Builder.Default
    private List<Teacher> teachers = new ArrayList<>();

    // Cardinality: Department 1..M Course
    @JsonIgnore
    @OneToMany(mappedBy = "department")
    @ToString.Exclude
    @Builder.Default
    private List<Course> courses = new ArrayList<>();
}
