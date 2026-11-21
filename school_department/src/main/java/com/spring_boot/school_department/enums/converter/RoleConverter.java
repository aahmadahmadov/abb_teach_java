package com.spring_boot.school_department.enums.converter;

import com.spring_boot.school_department.enums.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter implements AttributeConverter<Role, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Role attribute) {
        return attribute != null ? attribute.dbCode() : null;
    }

    @Override
    public Role convertToEntityAttribute(Integer dbData) {
        return dbData != null ? Role.findByDbCode(dbData) : null;
    }
}
