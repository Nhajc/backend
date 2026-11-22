package com.nhajc.server.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.hibernate.exception.ConstraintViolationException;

public final class DatabaseConflicts {
    private DatabaseConflicts() {}

    public static RuntimeException translate(DataIntegrityViolationException exception, String detail) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && "23505".equals(violation.getSQLState())) {
                return new ResponseStatusException(HttpStatus.CONFLICT, detail);
            }
        }
        return exception;
    }
}
