package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateCourseRequest;
import com.spring_boot.school_department.dto.request.UpdateCourseRequest;
import com.spring_boot.school_department.dto.response.CourseResponse;
import com.spring_boot.school_department.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "students", ignore = true)
    Course toEntity(CreateCourseRequest course);

    CourseResponse toResponse(Course course);

    List<CourseResponse> toResponseList(List<Course> courses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "students", ignore = true)
    void updateEntity(UpdateCourseRequest request, @MappingTarget Course course);
}
