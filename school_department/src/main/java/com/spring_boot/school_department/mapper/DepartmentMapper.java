package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateDepartmentRequest;
import com.spring_boot.school_department.dto.request.UpdateDepartmentRequest;
import com.spring_boot.school_department.dto.response.DepartmentResponse;
import com.spring_boot.school_department.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {EmployeeMapper.class})
public interface DepartmentMapper {

    @Mapping(target = "id", ignore = true)
    Department toEntity(CreateDepartmentRequest department);

    DepartmentResponse toResponse(Department department);

    List<DepartmentResponse> toResponseList(List<Department> departments);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UpdateDepartmentRequest request, @MappingTarget Department department);
}
