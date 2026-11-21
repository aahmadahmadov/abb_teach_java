package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateEmployeeRequest;
import com.spring_boot.school_department.dto.request.UpdateEmployeeRequest;
import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    EmployeeResponse toResponse(Employee employee);

    List<EmployeeResponse> toResponseList(List<Employee> employees);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    void updateEntity(UpdateEmployeeRequest request, @MappingTarget Employee employee);
}
