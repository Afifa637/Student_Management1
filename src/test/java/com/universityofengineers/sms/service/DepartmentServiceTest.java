package com.universityofengineers.sms.service;

import com.universityofengineers.sms.dto.request.DepartmentUpsertRequest;
import com.universityofengineers.sms.dto.response.DepartmentResponse;
import com.universityofengineers.sms.entity.Department;
import com.universityofengineers.sms.exception.BadRequestException;
import com.universityofengineers.sms.exception.ResourceNotFoundException;
import com.universityofengineers.sms.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @InjectMocks private DepartmentService departmentService;

    @Test
    void create_shouldUppercaseCode_trimFields_andPersist() {
        DepartmentUpsertRequest req = new DepartmentUpsertRequest();
        req.setCode("  cse  ");
        req.setName("  Computer Science  ");

        when(departmentRepository.existsByCode("CSE")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });

        DepartmentResponse res = departmentService.create(req);

        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getCode()).isEqualTo("CSE");
        assertThat(res.getName()).isEqualTo("Computer Science");

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("CSE");
        assertThat(captor.getValue().getName()).isEqualTo("Computer Science");
    }

    @Test
    void create_shouldRejectDuplicateDepartmentCode_caseInsensitive() {
        DepartmentUpsertRequest req = new DepartmentUpsertRequest();
        req.setCode("cse");
        req.setName("Computer Science");

        when(departmentRepository.existsByCode("CSE")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("code already exists");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_shouldAllowSameCode_withoutUniquenessQuery() {
        long id = 1L;
        Department existing = Department.builder().id(id).code("CSE").name("Old").build();

        DepartmentUpsertRequest req = new DepartmentUpsertRequest();
        req.setCode("  cse ");
        req.setName("  New Name ");

        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentResponse res = departmentService.update(id, req);


        assertThat(res.getCode()).isEqualTo("CSE");
        assertThat(res.getName()).isEqualTo("New Name");
        verify(departmentRepository, never()).existsByCode(any());
    }

    @Test
    void update_shouldRejectChangingCode_toExistingCode() {
        long id = 1L;
        Department existing = Department.builder().id(id).code("CSE").name("Old").build();

        DepartmentUpsertRequest req = new DepartmentUpsertRequest();
        req.setCode("EEE");
        req.setName("New");

        when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCode("EEE")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.update(id, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("code already exists");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void get_shouldThrow404_whenNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found");
    }

    @Test
    void list_shouldMapEntities_toDtos() {
        when(departmentRepository.findAll()).thenReturn(List.of(
                Department.builder().id(1L).code("CSE").name("CSE").build(),
                Department.builder().id(2L).code("EEE").name("EEE").build()
        ));

        List<DepartmentResponse> list = departmentService.list();

        assertThat(list).hasSize(2);
        assertThat(list).extracting(DepartmentResponse::getCode)
                .containsExactly("CSE", "EEE");
    }
}
