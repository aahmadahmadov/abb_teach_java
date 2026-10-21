package com.spring_boot.jpa_practice_1.entity.converter;

import com.spring_boot.jpa_practice_1.enums.GenderTypeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<GenderTypeEnum, Integer> {


    /**
     * Converts the value stored in the entity attribute into the
     * data representation to be stored in the database.
     *
     * @param attribute the entity attribute value to be converted
     * @return the converted data to be stored in the database column
     */
    @Override
    public Integer convertToDatabaseColumn(GenderTypeEnum attribute) {
        return attribute != null ? attribute.getId() : null;
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
    public GenderTypeEnum convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }

        return GenderTypeEnum.fromId(dbData);
    }
}
