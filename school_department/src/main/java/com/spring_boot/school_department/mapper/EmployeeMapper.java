package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateEmployeeRequest;
import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    EmployeeResponse toResponse(Employee employee);

    List<EmployeeResponse> toResponseList(List<Employee> employees);
}
