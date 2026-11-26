package com.nhajc.server.common.service;

import com.nhajc.server.user.entity.User;
import com.nhajc.server.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserAccess {
    private final UserRepository users;

    public UserAccess(UserRepository users) {
        this.users = users;
    }

    public User require(UUID actor) {
        return users.findById(actor).orElseThrow(UserAccess::missing);
    }

    public User lock(UUID actor) {
        return users.findLockedById(actor).orElseThrow(UserAccess::missing);
    }

    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists.");
    }
}
