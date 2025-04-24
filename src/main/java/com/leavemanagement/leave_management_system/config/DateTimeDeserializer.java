package com.leavemanagement.leave_management_system.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@JsonComponent
public class DateTimeDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText().trim();

        // First try to parse as ZonedDateTime
        try {
            return ZonedDateTime.parse(dateString);
        } catch (DateTimeParseException e) {
            // If that fails, try LocalDateTime
            try {
                return LocalDateTime.parse(dateString);
            } catch (DateTimeParseException e1) {
                // Try with ISO format
                try {
                    return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
                } catch (DateTimeParseException e2) {
                    // Last try with a flexible formatter
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]");
                        return ZonedDateTime.parse(dateString, formatter);
                    } catch (DateTimeParseException e3) {
                        throw new IOException("Could not parse date: " + dateString, e3);
                    }
                }
            }
        }
    }
}