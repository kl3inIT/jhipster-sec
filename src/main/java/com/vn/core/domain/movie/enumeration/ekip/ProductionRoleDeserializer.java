package com.vn.core.domain.movie.enumeration.ekip;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class ProductionRoleDeserializer extends JsonDeserializer<ProductionRole> {

    @Override
    public ProductionRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return ProductionRole.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // If not found by name, return null or throw exception based on requirement
            throw new IllegalArgumentException("Invalid ProductionRole value: " + value);
        }
    }
}
