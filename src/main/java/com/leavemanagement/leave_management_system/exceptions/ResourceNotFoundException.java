package com.leavemanagement.leave_management_system.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ResourceNotFoundException forResource(String resourceName, String fieldName, Object fieldValue) {
        String message = String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue);
        return new ResourceNotFoundException(message);
    }
}
