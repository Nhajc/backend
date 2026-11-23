package com.nhajc.server.common.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

public final class Actors {
    private Actors() {}

    public static UUID id(Jwt jwt) {
        return id(jwt == null ? null : jwt.getSubject());
    }

    public static UUID id(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token.");
        }
    }
}
