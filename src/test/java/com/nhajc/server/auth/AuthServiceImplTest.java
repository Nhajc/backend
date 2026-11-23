package com.nhajc.server.auth;

import com.nhajc.server.auth.dto.LoginRequest;
import com.nhajc.server.auth.dto.RegisterRequest;
import com.nhajc.server.auth.entity.RefreshToken;
import com.nhajc.server.auth.repository.RefreshTokenRepository;
import com.nhajc.server.auth.service.JwtService;
import com.nhajc.server.auth.service.impl.AuthServiceImpl;
import com.nhajc.server.user.dto.UserResponse;
import com.nhajc.server.user.entity.User;
import com.nhajc.server.user.mapper.UserResponseMapper;
import com.nhajc.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import org.hibernate.exception.ConstraintViolationException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    private UserRepository users;
    private RefreshTokenRepository tokens;
    private PasswordEncoder passwords;
    private JwtService jwt;
    private UserResponseMapper responses;
    private AuthServiceImpl auth;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(RefreshTokenRepository.class);
        passwords = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        responses = mock(UserResponseMapper.class);
        when(passwords.encode(anyString())).thenReturn("dummy-hash");
        auth = new AuthServiceImpl(users, tokens, passwords, jwt, responses);
    }

    @Test
    void missingAccountStillChecksBcryptAndUsesSameErrorAsWrongPassword() {
        assertInvalidLogin("missing", "wrong");
        verify(passwords).matches("wrong", "dummy-hash");

        when(users.findByUsernameIgnoreCase("exists")).thenReturn(Optional.of(user("exists")));
        assertInvalidLogin("exists", "wrong");
        verify(passwords).matches("wrong", "real-hash");
        verify(passwords, times(1)).encode(anyString());
        verifyNoInteractions(tokens);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void overlongUtf8PasswordStillChecksSafeDummyPassword(boolean existingUser) {
        when(users.findByUsernameIgnoreCase("dai"))
                .thenReturn(existingUser ? Optional.of(user("dai")) : Optional.empty());
        assertInvalidLogin("dai", "€".repeat(25));
        verify(passwords).matches(argThat(value -> value.length() <= 72), eq("dummy-hash"));
        verifyNoInteractions(tokens);
    }

    @Test
    void identifierContainingAtUsesEmailOnly() {
        User owner = user("owner");
        when(users.findByEmailIgnoreCase("shared@example.com")).thenReturn(Optional.of(owner));
        when(passwords.matches("secret", "real-hash")).thenReturn(true);
        when(jwt.createAccessToken(owner)).thenReturn("jwt");
        when(responses.from(owner)).thenReturn(UserResponse.from(owner, null));

        var response = auth.login(new LoginRequest(" shared@example.com ", "secret"));

        assertThat(response.accessToken()).isEqualTo("jwt");
        assertThat(response.refreshToken()).hasSize(43);
        assertThat(response.user().id()).isEqualTo(owner.getId());
        verify(users, never()).findByUsernameIgnoreCase(anyString());
        verify(tokens).save(any(RefreshToken.class));
    }

    @Test
    void wrongEmailPasswordDoesNotTryUsername() {
        when(users.findByEmailIgnoreCase("shared@example.com")).thenReturn(Optional.of(user("owner")));
        assertInvalidLogin("shared@example.com", "wrong");
        verify(users, never()).findByUsernameIgnoreCase(anyString());
        verifyNoInteractions(tokens);
    }

    @Test
    void missingEmailDoesNotFallbackToUsername() {
        assertInvalidLogin("missing@example.com", "wrong");
        verify(users).findByEmailIgnoreCase("missing@example.com");
        verify(users, never()).findByUsernameIgnoreCase(anyString());
    }

    @Test
    void ordinaryUsernameDoesNotQueryEmail() {
        assertInvalidLogin("dai", "wrong");
        verify(users, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void registrationRejectsExistingEmail() {
        when(users.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);
        assertThatThrownBy(() -> auth.register(new RegisterRequest("newuser", "existing@example.com", "x".repeat(12))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(409));
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(tokens);
    }

    @Test
    void registrationRejectsPasswordAbove72Utf8Bytes() {
        assertThatThrownBy(() -> auth.register(new RegisterRequest("dai", "dai@example.com", "€".repeat(25))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(users, tokens);
    }

    @Test
    void registrationAcceptsExactly72Utf8Bytes() {
        String password = "€".repeat(24);
        auth.register(new RegisterRequest("dai", "dai@example.com", password));
        verify(passwords).encode(password);
        verify(users).saveAndFlush(any(User.class));
        verify(tokens).save(any(RefreshToken.class));
    }

    @Test
    void concurrentDuplicateRegistrationStillReturnsConflict() {
        var violation = mock(ConstraintViolationException.class);
        when(violation.getSQLState()).thenReturn("23505");
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException(
                "duplicate", violation));
        assertThatThrownBy(() -> auth.register(new RegisterRequest("dai", "dai@example.com", "x".repeat(12))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(409));
        verifyNoInteractions(tokens);
    }

    private void assertInvalidLogin(String identifier, String password) {
        assertThatThrownBy(() -> auth.login(new LoginRequest(identifier, password)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(401);
                    assertThat(exception.getReason()).isEqualTo("Invalid username, email or password.");
                });
    }

    private static User user(String username) {
        User user = new User(username, "owner@example.com", "real-hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
