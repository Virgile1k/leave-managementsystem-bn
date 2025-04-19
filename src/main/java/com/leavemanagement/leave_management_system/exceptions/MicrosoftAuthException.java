package com.leavemanagement.leave_management_system.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class MicrosoftAuthException extends RuntimeException {
    public MicrosoftAuthException(String message) {
        super(message);
    }

    public MicrosoftAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
