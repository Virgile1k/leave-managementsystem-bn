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
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText().trim();

        try {
            // First try to parse as ZonedDateTime and convert to LocalDateTime
            return ZonedDateTime.parse(dateString).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                // Then try as LocalDateTime directly
                return LocalDateTime.parse(dateString);
            } catch (DateTimeParseException ex) {
                try {
                    // Try with standard ISO formatter
                    return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
                } catch (DateTimeParseException ex2) {
                    throw new IOException("Unable to parse date: " + dateString, ex2);
                }
            }
        }
    }
}