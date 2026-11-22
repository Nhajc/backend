package com.nhajc.server.common.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

@Component
public class MultipartRequests {
    private final Validator validator;
    public MultipartRequests(Validator validator) { this.validator = validator; }

    public <T> T validate(T value) {
        var errors = validator.validate(value);
        if (!errors.isEmpty()) {
            String detail = errors.stream().map(error -> error.getPropertyPath() + ": " + error.getMessage())
                    .sorted().collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
        }
        return value;
    }

    public void requireFilePart(HttpServletRequest request, String name, MultipartFile file) {
        if (file == null && request.getParameterMap().containsKey(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    name + " must be a File part in multipart/form-data, not text or a file path.");
        }
    }
}
