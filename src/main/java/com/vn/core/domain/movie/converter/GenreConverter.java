package com.vn.core.domain.movie.converter;


import com.vn.core.domain.movie.enumeration.Genre;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenreConverter implements AttributeConverter<Genre, String> {

    @Override
    public String convertToDatabaseColumn(Genre attribute) {
        if (attribute == null) {
            return null;
        }
        // Save to DB as the STRING name of the ENUM (e.g. "ACTION")
        return attribute.name();
    }

    @Override
    public Genre convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        try {
            // Check if it's the old numeric format (e.g. "0")
            int ordinal = Integer.parseInt(dbData);
            Genre[] values = Genre.values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal]; // Map 0 -> ACTION, 1 -> COMEDY, etc
            }
            return null;
        } catch (NumberFormatException e) {
            // Otherwise, it must be the new string format (e.g. "ACTION")
            try {
                return Genre.valueOf(dbData);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}
