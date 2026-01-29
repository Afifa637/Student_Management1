package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.CourseUpsertRequest;
import com.universityofengineers.sms.dto.response.CourseResponse;
import com.universityofengineers.sms.dto.response.DepartmentResponse;
import com.universityofengineers.sms.dto.response.TeacherResponse;
import com.universityofengineers.sms.entity.Course;
import com.universityofengineers.sms.entity.EnrollmentStatus;
import com.universityofengineers.sms.entity.Teacher;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ForbiddenException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.CourseRepository;
import com.universityofengineers.sms.repository.DepartmentRepository;
import com.universityofengineers.sms.repository.EnrollmentRepository;
import com.universityofengineers.sms.repository.TeacherRepository;
import com.universityofengineers.sms.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> list() {
        return courseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse get(Long id) {
        Course c = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        return toResponse(c);
    }

    @Transactional
    public CourseResponse create(CourseUpsertRequest req) {
        String code = req.getCode().trim().toUpperCase();
        if (courseRepository.existsByCode(code)) {
            throw new BadRequestException("Course code already exists.");
        }

        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        Teacher currentTeacher = currentTeacher();
        // Practical authorization: teachers manage courses only in their own department.
        if (!currentTeacher.getDepartment().getId().equals(dept.getId())) {
            throw new ForbiddenException("You can only create courses within your own department.");
        }

        Teacher assignedTeacher = (req.getTeacherId() != null)
                ? teacherRepository.findById(req.getTeacherId()).orElseThrow(() -> new ResourceNotFoundException("Teacher not found."))
                : currentTeacher;

        // Practical constraint: assigned teacher must match the course department.
        if (!assignedTeacher.getDepartment().getId().equals(dept.getId())) {
            throw new BadRequestException("Assigned teacher must belong to the same department as the course.");
        }

        Course c = Course.builder()
                .code(code)
                .title(req.getTitle().trim())
                .credit(req.getCredit())
                .capacity(req.getCapacity())
                .department(dept)
                .teacher(assignedTeacher)
                .build();

        return toResponse(courseRepository.save(c));
    }

    @Transactional
    public CourseResponse update(Long id, CourseUpsertRequest req) {
        Course c = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found."));

        Teacher currentTeacher = currentTeacher();

        // Practical authorization: teachers manage courses only in their own department.
        // (So they can't update other departments' courses.)
        if (!currentTeacher.getDepartment().getId().equals(c.getDepartment().getId())) {
            throw new ForbiddenException("You can only update courses in your own department.");
        }

        String code = req.getCode().trim().toUpperCase();
        if (!c.getCode().equals(code) && courseRepository.existsByCode(code)) {
            throw new BadRequestException("Course code already exists.");
        }

        var dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (!currentTeacher.getDepartment().getId().equals(dept.getId())) {
            throw new ForbiddenException("You can only move/update courses within your own department.");
        }

        Teacher assignedTeacher = (req.getTeacherId() != null)
                ? teacherRepository.findById(req.getTeacherId()).orElseThrow(() -> new ResourceNotFoundException("Teacher not found."))
                : c.getTeacher();

        if (!assignedTeacher.getDepartment().getId().equals(dept.getId())) {
            throw new BadRequestException("Assigned teacher must belong to the same department as the course.");
        }

        long currentlyEnrolled = enrollmentRepository.countByCourseIdAndStatus(c.getId(), EnrollmentStatus.ENROLLED);
        if (req.getCapacity() < currentlyEnrolled) {
            throw new BadRequestException("Capacity cannot be less than current enrolled count (" + currentlyEnrolled + ").");
        }

        c.setCode(code);
        c.setTitle(req.getTitle().trim());
        c.setCredit(req.getCredit());
        c.setCapacity(req.getCapacity());
        c.setDepartment(dept);
        c.setTeacher(assignedTeacher);

        return toResponse(courseRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Course c = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found."));

        Teacher currentTeacher = currentTeacher();
        if (!currentTeacher.getDepartment().getId().equals(c.getDepartment().getId())) {
            throw new ForbiddenException("You can only delete courses in your own department.");
        }

        courseRepository.delete(c);
    }

    private Teacher currentTeacher() {
        Long accountId = SecurityUtils.currentAccountId();
        return teacherRepository.findByAccountId(accountId).orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found."));
    }

    private CourseResponse toResponse(Course c) {
        DepartmentResponse dept = DepartmentResponse.builder()
                .id(c.getDepartment().getId())
                .code(c.getDepartment().getCode())
                .name(c.getDepartment().getName())
                .build();

        TeacherResponse teacher = TeacherResponse.builder()
                .id(c.getTeacher().getId())
                .employeeNo(c.getTeacher().getEmployeeNo())
                .fullName(c.getTeacher().getFullName())
                .email(c.getTeacher().getAccount().getEmail())
                .title(c.getTeacher().getTitle())
                .department(DepartmentResponse.builder()
                        .id(c.getTeacher().getDepartment().getId())
                        .code(c.getTeacher().getDepartment().getCode())
                        .name(c.getTeacher().getDepartment().getName())
                        .build())
                .build();

        long currentlyEnrolled = enrollmentRepository.countByCourseIdAndStatus(c.getId(), EnrollmentStatus.ENROLLED);

        return CourseResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .title(c.getTitle())
                .credit(c.getCredit())
                .capacity(c.getCapacity())
                .department(dept)
                .teacher(teacher)
                .currentlyEnrolled(currentlyEnrolled)
                .build();
    }
}
