package com.spring_boot.school_department.enums.converter;

import com.spring_boot.school_department.enums.GenderEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<GenderEnum, Integer> {

    /**
     * Converts the value stored in the entity attribute into the
     * data representation to be stored in the database.
     *
     * @param attribute the entity attribute value to be converted
     * @return the converted data to be stored in the database column
     */
    @Override
    public Integer convertToDatabaseColumn(GenderEnum attribute) {
        return attribute != null ? attribute.dbCode() : null;
    }

    /**
     * Converts the data stored in the database column into the value
     * to be stored in the entity attribute.
     *
     * <p>Note that it is the responsibility of the converter writer
     * to specify the correct {@code dbData} type for the corresponding
     * column for use by the JDBC driver: i.e., persistence providers
     * are not expected to do such type conversion.
     *
     * @param dbData the data from the database column to be converted
     * @return the converted value to be stored in the entity attribute
     */
    @Override
    public GenderEnum convertToEntityAttribute(Integer dbData) {
        return dbData != null ? GenderEnum.findByDbCode(dbData) : null;
    }
}
