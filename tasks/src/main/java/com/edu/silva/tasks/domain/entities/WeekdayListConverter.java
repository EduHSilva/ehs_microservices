package com.edu.silva.tasks.domain.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Stores the ISO weekdays (1 = Monday .. 7 = Sunday) as a compact CSV column. */
@Converter
public class WeekdayListConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .distinct()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return new ArrayList<>(Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::valueOf)
                .toList());
    }
}
