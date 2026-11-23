package com.nhajc.server.auth.service.impl;

import com.nhajc.server.auth.dto.AuthResponse;
import com.nhajc.server.auth.dto.LoginRequest;
import com.nhajc.server.auth.dto.RefreshTokenRequest;
import com.nhajc.server.auth.dto.RegisterRequest;
import com.nhajc.server.auth.entity.RefreshToken;
import com.nhajc.server.auth.repository.RefreshTokenRepository;
import com.nhajc.server.auth.service.AuthService;
import com.nhajc.server.auth.service.JwtService;
import com.nhajc.server.user.entity.User;
import com.nhajc.server.user.mapper.UserResponseMapper;
import com.nhajc.server.user.repository.UserRepository;
import com.nhajc.server.common.exception.DatabaseConflicts;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid username, email or password.";
    private static final String DUMMY_PASSWORD = "unused-login-password";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final JwtService jwtService;
    private final UserResponseMapper responses;
    private final String dummyPasswordHash;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwords, JwtService jwtService, UserResponseMapper responses) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.jwtService = jwtService;
        this.responses = responses;
        this.dummyPasswordHash = passwords.encode(DUMMY_PASSWORD);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().strip();
        String email = request.email().strip();

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at most 72 UTF-8 bytes.");
        }

        if (users.existsByUsernameIgnoreCase(username) || users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username or email already exists.");
        }

        User user = new User(username, email, passwords.encode(request.password()));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw DatabaseConflicts.translate(exception, "Username or email already exists.");
        }

        return issueTokens(user, UUID.randomUUID());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().strip();
        User user = (identifier.contains("@")
                ? users.findByEmailIgnoreCase(identifier)
                : users.findByUsernameIgnoreCase(identifier)).orElse(null);

        boolean tooLong = request.password().getBytes(StandardCharsets.UTF_8).length > 72;
        String hash = user == null || tooLong ? dummyPasswordHash : user.getPassword();
        // BCrypt rejects inputs over 72 bytes. Still perform one safe comparison.
        boolean matches = passwords.matches(tooLong ? DUMMY_PASSWORD : request.password(), hash);
        if (user == null || tooLong || !matches) {
            throw unauthorized(INVALID_CREDENTIALS);
        }
        return issueTokens(user, UUID.randomUUID());
    }

    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = hash(request.refreshToken());
        UUID userId = refreshTokens.findUserIdByTokenHash(hash)
                .orElseThrow(() -> unauthorized("Invalid refresh token."));

        // Lock before reading token state so concurrent refresh requests serialize.
        User user = users.findLockedById(userId)
                .orElseThrow(() -> unauthorized("Invalid refresh token."));
        RefreshToken token = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> unauthorized("Invalid refresh token."));

        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            refreshTokens.deleteFamily(token.getFamilyId());
            // noRollbackFor preserves this revocation when returning HTTP 401.
            throw unauthorized("Refresh token expired or already used. Please log in again.");
        }

        token.markUsed(now);
        return issueTokens(user, token.getFamilyId());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = hash(request.refreshToken());
        var userId = refreshTokens.findUserIdByTokenHash(hash);
        if (userId.isEmpty() || users.findLockedById(userId.get()).isEmpty()) {
            return;
        }

        refreshTokens.findByTokenHash(hash)
                .ifPresent(token -> refreshTokens.deleteFamily(token.getFamilyId()));
    }

    private AuthResponse issueTokens(User user, UUID familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken(user.getId(), familyId, hash(rawToken),
                Instant.now().plus(Duration.ofDays(30)));
        refreshTokens.save(token);

        return new AuthResponse(jwtService.createAccessToken(user), rawToken,
                responses.from(user));
    }

    private static String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ResponseStatusException unauthorized(String detail) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, detail);
    }

}
