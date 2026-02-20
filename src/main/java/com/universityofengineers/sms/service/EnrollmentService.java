package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.EnrollmentCreateRequest;
import com.universityofengineers.sms.dto.request.GradeUpdateRequest;
import com.universityofengineers.sms.dto.response.EnrollmentResponse;
import com.universityofengineers.sms.entity.*;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ForbiddenException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.CourseRepository;
import com.universityofengineers.sms.repository.EnrollmentRepository;
import com.universityofengineers.sms.repository.StudentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
import com.universityofengineers.sms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myEnrollments() {
        Student me = getCurrentStudent();
        return enrollmentRepository.findByStudentId(me.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EnrollmentResponse enrollMe(EnrollmentCreateRequest req) {
        Student me = getCurrentStudent();
        return enrollStudentToCourse(me.getId(), req.getCourseId(), true);
    }

    @Transactional
    public void dropMyEnrollment(Long enrollmentId) {
        Student me = getCurrentStudent();
        Enrollment e = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        if (!e.getStudent().getId().equals(me.getId())) {
            throw new ForbiddenException("You can only drop your own enrollments.");
        }
        if (e.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new BadRequestException("Enrollment is not active.");
        }

        e.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listAll() {
        return enrollmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EnrollmentResponse teacherEnrollStudent(Long studentId, EnrollmentCreateRequest req) {
        // Practical authorization: teacher can enroll students only into the courses they teach.
        Course course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        Teacher currentTeacher = teacherRepository.findByAccountId(SecurityUtils.currentAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found."));
        if (!course.getTeacher().getId().equals(currentTeacher.getId())) {
            throw new ForbiddenException("You can only enroll students into your own courses.");
        }

        return enrollStudentToCourse(studentId, req.getCourseId(), false);
    }

    @Transactional
    public EnrollmentResponse setGrade(Long enrollmentId, GradeUpdateRequest req) {
        Enrollment e = enrollmentRepository.findById(enrollmentId).orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));
        // Practical authorization: a teacher can grade ONLY the courses they teach.
        Teacher currentTeacher = teacherRepository.findByAccountId(SecurityUtils.currentAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found."));
        if (!e.getCourse().getTeacher().getId().equals(currentTeacher.getId())) {
            throw new ForbiddenException("You can only grade enrollments for your own courses.");
        }

        if (e.getStatus() == EnrollmentStatus.DROPPED) {
            throw new BadRequestException("Cannot grade a dropped enrollment.");
        }
        e.setGrade(req.getGrade().trim().toUpperCase());
        // Practical: if grade is set and enrollment is still ENROLLED, mark COMPLETED
        if (e.getStatus() == EnrollmentStatus.ENROLLED) {
            e.setStatus(EnrollmentStatus.COMPLETED);
        }
        return toResponse(enrollmentRepository.save(e));
    }

    private EnrollmentResponse enrollStudentToCourse(Long studentId, Long courseId, boolean initiatedByStudent) {
        Student s = studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        if (initiatedByStudent && s.getStatus() != StudentStatus.ACTIVE) {
            throw new ForbiddenException("Only ACTIVE students can enroll.");
        }

        Course c = courseRepository.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course not found."));

        var existingOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            if (existing.getStatus() == EnrollmentStatus.ENROLLED) {
                throw new BadRequestException("Already enrolled in this course.");
            }
            if (existing.getStatus() == EnrollmentStatus.COMPLETED) {
                throw new BadRequestException("Course already completed; re-enrollment is not allowed.");
            }
            // If it was DROPPED, re-activate the same record (keeps unique constraint happy)
            if (existing.getStatus() == EnrollmentStatus.DROPPED) {
                long enrolledCount = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED);
                if (enrolledCount >= c.getCapacity()) {
                    throw new BadRequestException("Course capacity reached.");
                }
                existing.setStatus(EnrollmentStatus.ENROLLED);
                existing.setGrade(null);
                return toResponse(enrollmentRepository.save(existing));
            }
        }

        long enrolledCount = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED);
        if (enrolledCount >= c.getCapacity()) {
            throw new BadRequestException("Course capacity reached.");
        }

        Enrollment e = Enrollment.builder()
                .student(s)
                .course(c)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        return toResponse(enrollmentRepository.save(e));
    }

    private Student getCurrentStudent() {
        if (!SecurityUtils.isStudent()) {
            throw new ForbiddenException("Only students can access this operation.");
        }
        return studentRepository.findByAccountId(SecurityUtils.currentAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentNo(e.getStudent().getStudentNo())
                .courseId(e.getCourse().getId())
                .courseCode(e.getCourse().getCode())
                .courseTitle(e.getCourse().getTitle())
                .status(e.getStatus())
                .grade(e.getGrade())
                .enrolledAt(e.getEnrolledAt())
                .build();
    }
}