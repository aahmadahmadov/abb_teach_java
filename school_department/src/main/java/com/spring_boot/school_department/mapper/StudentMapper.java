package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateStudentRequest;
import com.spring_boot.school_department.dto.request.UpdateStudentRequest;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courses", ignore = true)
    Student toEntity(CreateStudentRequest student);

    @Mapping(target = "courses", ignore = true)
    StudentResponse toResponse(Student student);

    @Mapping(target = "courses", ignore = true)
    List<StudentResponse> toResponseList(List<Student> students);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courses", ignore = true)
    void updateEntity(UpdateStudentRequest request, @MappingTarget Student student);
}
